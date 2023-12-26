package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.example.dao.RawDataDAO;
import org.example.entity.RawData;
import org.example.service.MainService;
import org.example.service.ProducerService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
@Log4j
@RequiredArgsConstructor
@Service
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;

    @Override
    public void processTextMessage(Update update) {
            saveRawData(update);
        var message = update.getMessage();
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Hello from NODE");
        producerService.produceAnswer(sendMessage);
    }

    @Override
    public void processDocMessage(Update update) {

    }

    @Override
    public void processPhotoMessage(Update update) {

    }

    private void saveRawData(Update update) {
            var rawData = RawData.builder()
                    .event(update)
                    .build();
            rawDataDAO.save(rawData);
    }
}
