package com.fuelnet.fuelnet.services;

import com.fuelnet.fuelnet.models.FuelAlert;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async("taskExecutor")
    public void sendLowFuelAlert(String toEmail, FuelAlert alert) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                    "⚠️ Alerta de combustible crítico — " +
                            alert.getTank().getFuelType());
            helper.setText(buildAlertInventoryHtml(alert), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println(
                    "Error enviando correo de alerta: " + e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendEmailVerification(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Fuelnet | Verifica tu email");
            helper.setText(buildActivationEmail(toEmail, token), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendSuccessUserVerification(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Fuelnet | Cuenta Verificada");
            helper.setText(buildUserActivationEmail(toEmail), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendPlatformAdminReviewMail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setSubject("Fuelnet | Cuenta necesita revision");
            helper.setText(buildAdminMessageToSeeReviews(), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendWaitingForRevision(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Fuelnet | Cuenta necesita revision");
            helper.setText(buildAdminReviewEmail(toEmail), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendApproveByAdmin(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Fuelnet | Cuenta Aprovada");
            helper.setText(buildApprovedAccountEmail(toEmail), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendRejectedByAdmin(String toEmail, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Fuelnet | Cuenta Rechazada");
            helper.setText(buildRejectedAccountEmail(toEmail, reason), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String buildAlertInventoryHtml(FuelAlert alert) {
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
                alert.getCreatedAt());
    }

    private String buildActivationEmail(String name, String token) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #1e88e5;">📩 Verifica tu correo</h2>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>Gracias por registrarte en nuestro sistema. Para poder acceder, debes verificar tu correo electrónico.</p>

                    <p style="margin-top: 20px;">
                        Haz clic en el siguiente enlace para activar tu cuenta:
                    </p>

                    <a href="http://10.2.2.2:3015/api/auth/activate?token=%s"
                       style="display:inline-block;padding:10px 15px;background:#1e88e5;color:white;text-decoration:none;border-radius:5px;">
                       Activar cuenta
                    </a>

                    <p style="margin-top: 20px; color: #888;">
                        Si no solicitaste esta cuenta, puedes ignorar este mensaje.
                    </p>
                </div>
                """
                .formatted(name, token);
    }

    private String buildUserActivationEmail(String name) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #1e88e5;">📩 Verifica tu correo</h2>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>Tu cuenta ha sido verificafa correctamente</p>

                    <p style="margin-top: 20px;">
                        Ya puedes iniciar sesion
                    </p>

                </div>
                """
                .formatted(name);
    }

    private String buildAdminMessageToSeeReviews() {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #fb8c00;">⏳ Cuenta en revisión</h2>

                    <p>Hola <strong>PLATFORM_ADMIN</strong>,</p>

                    <p>Una nueva cuenta de administrador quiere ser registrada</p>

                    <p>Entra a la aplicacion para revisarla.</p>
                </div>
                """;
    }

    private String buildAdminReviewEmail(String name) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #fb8c00;">⏳ Cuenta en revisión</h2>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>Tu solicitud de cuenta ha sido recibida correctamente.</p>

                    <p>Un administrador está revisando tus credenciales para aprobar tu acceso al sistema.</p>

                    <p style="margin-top: 16px;">
                        Te notificaremos por correo una vez se tome una decisión.
                    </p>

                    <p style="color: #888;">
                        Este proceso puede tardar unos minutos u horas dependiendo de la validación.
                    </p>
                </div>
                """.formatted(name);
    }

    private String buildApprovedAccountEmail(String email) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #43a047;">✅ Cuenta aprobada</h2>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>Tu cuenta ha sido <strong>aprobada</strong> por el administrador.</p>

                    <p>A continuación tus credenciales de acceso:</p>

                    <table style="width:100%%; border-collapse: collapse; margin-top: 10px;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Usuario</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Contraseña que tu pusiste</strong></td>
                        </tr>
                    </table>
                </div>
                """
                .formatted(email, email);
    }

    private String buildRejectedAccountEmail(String name, String reason) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #e53935;">❌ Solicitud rechazada</h2>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>Lamentamos informarte que tu solicitud de cuenta ha sido <strong>rechazada</strong>.</p>

                    <p><strong>Motivo:</strong> %s</p>

                    <p style="margin-top: 16px;">
                        Puedes corregir la información e intentar registrarte nuevamente.
                    </p>

                    <a href="https://tu-dominio.com/register"
                       style="display:inline-block;padding:10px 15px;background:#e53935;color:white;text-decoration:none;border-radius:5px;">
                       Intentar nuevamente
                    </a>
                </div>
                """
                .formatted(name, reason);
    }
}
