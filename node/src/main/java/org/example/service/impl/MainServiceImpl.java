package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.example.dao.AppDocumentDAO;
import org.example.dao.AppUserDAO;
import org.example.dao.RawDataDAO;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.entity.AppUser;
import org.example.entity.RawData;
import org.example.exceptions.UploadFileException;
import org.example.service.FileService;
import org.example.service.MainService;
import org.example.service.ProducerService;
import org.example.service.enums.ServiceCommand;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.example.entity.enums.UserState.BASIC_STATE;
import static org.example.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static org.example.service.enums.ServiceCommand.*;

@Log4j
@RequiredArgsConstructor
@Service
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;

    @Override
    public void processTextMessage(Update update) {
            saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);
        if(CANCEL.equals(serviceCommand)){
            output = cancelProcess(appUser);
        }else if(BASIC_STATE.equals(userState)){
            output = processServiceCommand(appUser, text);
        }else if(WAIT_FOR_EMAIL_STATE.equals(userState)){
            //TODO добавить обработку email
        }else{
            log.error("Unknown user state: "+ userState);
            output = "Неизвестная ошибка! Введите /cancel и попробуйте снова! ";
        }
        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if(isNotAllowToSendContent(chatId,appUser)){
            return;
        }

        try{
            AppDocument doc = fileService.processDoc(update.getMessage());
            //TODO Добавить генерацию ссылки для скачивания документа
  var answer = "Документ успешно загружен! Ссылка для скачивания: https://test.com/get-doc/777";
            sendAnswer(answer, chatId);
        }catch (UploadFileException e){
            log.error(e);
            String error = "К сожалению, загрузка файла не удалась. Повторите попытку позже.";
            sendAnswer(error,chatId);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if(isNotAllowToSendContent(chatId,appUser)){
            return;
        }
        try{
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            //TODO Добавить генерацию ссылки для скачивания документа
            var answer = "Фото успешно загружен! Ссылка для скачивания: https://test.com/get-photo/777";
            sendAnswer(answer, chatId);
        }catch (UploadFileException e){
            log.error(e);
            String error = "К сожалению, загрузка фото не удалась. Повторите попытку позже.";
            sendAnswer(error,chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if(!appUser.getIsActive()){
            var error = "Зарегестрируйтесь или активируйте свою учетную запись для загрузки контента.";
          sendAnswer(error,chatId);
            return true;
        }else if(!BASIC_STATE.equals(userState)){
            var error = "Отмените текующую команду с помощью /cancel для отправки файлов.";
            sendAnswer(error,chatId);
            return true;
        }
        return false;
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        if(REGISTRATION.equals(cmd)){
            //TODO добавить регитстрацию
            return "Временно недоступно.";
        }else if(HELP.equals(cmd)){
            return help();
        }else if(START.equals(cmd)){
            return "Приветствую! Чтобы посмотреть список доступных команд введите /help";
        }else{
            return "Неизвестная команда! Чтобы посмотреть список доступных комманд введите /help";
        }
    }

    private String help() {
        return """
                Список доступных команд:
                    /cancel  - отмена выполнения текующей команды;
                    /registration - регистрация пользователя.;
                """;

    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Команда отменена! ";
    }


    private AppUser findOrSaveAppUser(Update update){
        User telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(telegramUser.getId());
            if(persistentAppUser==null){
                AppUser transientAppUser = AppUser.builder()
                        .telegramUserId(telegramUser.getId())
                        .userName(telegramUser.getUserName())
                        .firstName(telegramUser.getFirstName())
                        .lastName(telegramUser.getLastName())
                        //TODO изменить значение по умолчанию после добавления регистрации
                        .isActive(true)
                        .state(BASIC_STATE)
                        .build();
                return appUserDAO.save(transientAppUser);
            }
        return persistentAppUser;
    }
    private void saveRawData(Update update) {
            var rawData = RawData.builder()
                    .event(update)
                    .build();
            rawDataDAO.save(rawData);
    }
}
