package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.models.FuelAlert;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendLowFuelAlert(String toEmail, FuelAlert alert) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                true,
                "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                "⚠️ Alerta de combustible crítico — " +
                    alert.getTank().getFuelType()
            );
            helper.setText(buildEmailHtml(alert), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println(
                "Error enviando correo de alerta: " + e.getMessage()
            );
        }
    }

    private String buildEmailHtml(FuelAlert alert) {
        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
            <h2 style="color: #e53935;">⚠️ Nivel crítico de combustible</h2>
            <p>Se ha detectado un nivel bajo en uno de los tanques de tu estación.</p>
            <table style="width:100%%; border-collapse: collapse;">
                <tr>
                    <td style="padding: 8px; border: 1px solid #ddd;"><strong>Tipo</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                </tr>
                <tr>
                    <td style="padding: 8px; border: 1px solid #ddd;"><strong>Nivel actual</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">%s galones (%.2f%%)</td>
                </tr>
                <tr>
                    <td style="padding: 8px; border: 1px solid #ddd;"><strong>Umbral configurado</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">%.2f%%</td>
                </tr>
                <tr>
                    <td style="padding: 8px; border: 1px solid #ddd;"><strong>Fecha y hora</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                </tr>
            </table>
            <p style="margin-top: 16px; color: #888;">Por favor realiza una recarga lo antes posible.</p>
        </div>
        """.formatted(
                alert.getTank().getFuelType(),
                alert.getLevelAtAlert(),
                alert.getPercentageAtAlert(),
                alert.getThresholdUsed(),
                alert.getCreatedAt()
            );
    }
}
