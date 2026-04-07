package com.fuelnet.fuelnet.services;

import java.util.List;
import com.fuelnet.fuelnet.models.DeviceToken;
import com.fuelnet.fuelnet.repositories.IDeviceTokenRepository;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final IDeviceTokenRepository tokenRepository;

    public String sendToUser(Long userId, String title, String body) {

        List<DeviceToken> tokens = tokenRepository.findByUserId(userId);

        if (tokens.isEmpty())
            return "Failed";

        List<String> tokenList = tokens.stream()
                .map(DeviceToken::getToken)
                .toList();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenList)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging
                    .getInstance()
                    .sendEachForMulticast(message);

            List<SendResponse> responses = response.getResponses();

            for (int i = 0; i < responses.size(); i++) {

                if (!responses.get(i).isSuccessful()) {

                    String failedToken = tokenList.get(i);
                    FirebaseMessagingException e = responses.get(i).getException();

                    if (e.getErrorCode().equals("registration-token-not-registered")) {
                        tokenRepository.findByToken(failedToken)
                                .ifPresent(tokenRepository::delete);
                    }

                    System.out.println("❌ Error enviando a token: " + failedToken);
                    e.printStackTrace();
                }
            }

        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }

        return "Success";
    }

    public String sendToToken(String token, String title, String body)
            throws FirebaseMessagingException {

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        return FirebaseMessaging.getInstance().send(message);
    }

    public String sendToTopic(String topic, String title, String body)
            throws FirebaseMessagingException {

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        return FirebaseMessaging.getInstance().send(message);
    }
}
