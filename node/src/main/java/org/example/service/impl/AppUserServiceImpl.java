package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import org.example.dao.AppUserDAO;
import org.example.dto.MailParams;
import org.example.entity.AppUser;
import org.example.entity.enums.UserState;
import org.example.service.AppUserService;
import org.example.utils.CryptoTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static org.example.entity.enums.UserState.BASIC_STATE;
import static org.example.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
@Log4j
@RequiredArgsConstructor
@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserDAO appUserDAO;

    private final CryptoTool cryptoTool;
    @Value("${service.mail.uri}")
    private String mailServiceUri;


    @Override
    public String registerUser(AppUser appUser){
        if(appUser.getIsActive()){
            return "Вы уже зарегистрированы!";
        }else if(appUser.getEmail() !=null){
            return """
                    Вам на почту было отправлено письмо.
                    Перейдите по ссылке в письме для подтверждения регистрации.
                    """;
        }
        appUser.setState(WAIT_FOR_EMAIL_STATE);
        appUserDAO.save(appUser);
        return "Введите, пожалуйста, ваш email: ";
    }
    @Override
    public String setEmail(AppUser appUser, String email){
        try{
            var emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException e) {
            return "Введите, пожалуйста, корректный email. Для отмены команды введите /cancel";
        }
        var optional = appUserDAO.findByEmail(email);
        if(optional.isEmpty()){
            appUser.setEmail(email);
            appUser.setState(BASIC_STATE);
            appUser = appUserDAO.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            var response = sendRequestToMailService(cryptoUserId, email);
            if(response.getStatusCode()!= HttpStatus.OK){
                var msg = String.format("Отправка эл. письма на почту %s не удалось.",email);
                log.error(msg);
                appUser.setEmail(null);
                appUserDAO.save(appUser);
                return msg;
            }
            return "Вам на почту было отправлено письмо. "
                    +"Перейдите по ссылке в письме для подтверждения";
        }else{
            return "Этот email уже используется. Введите корректный email. "
                    +"Для отмены команды введите /cancel";

        }
    }

    private ResponseEntity<String> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        var request = new HttpEntity<MailParams>(mailParams, headers);
        return restTemplate.exchange(mailServiceUri,
                HttpMethod.POST,
                request,
                String.class);
    }
}
