package org.example.publickeyinfrastructure.Services.Auth;

import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.example.publickeyinfrastructure.Entities.User.RegularUser;
import org.springframework.beans.factory.annotation.Value;

import java.io.UnsupportedEncodingException;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.name:Public Key Infrastructure}")
    private String appName;
    
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    /**
     * Send verification email
     */
    public void sendVerificationEmail(RegularUser user, String verificationToken) {
        try {
            System.out.println("Attempting to send verification email to: " + user.getEmail());
            System.out.println("Using SMTP host: " + mailSender.toString());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
            System.out.println("Verification URL: " + verificationUrl);
            
            // Prepare template context
            Context context = new Context();
            context.setVariable("userName", user.getName() + " " + user.getSurname());
            context.setVariable("appName", appName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("supportEmail", fromEmail);
            
            // Generate HTML content using Thymeleaf
            System.out.println("Processing email template...");
            String htmlContent = templateEngine.process("email/verification", context);
            System.out.println("Template processed successfully, content length: " + htmlContent.length());
            
            helper.setFrom(fromEmail, appName);
            helper.setTo(user.getEmail());
            helper.setSubject("Confirm your email identity - " + appName);
            helper.setText(htmlContent, true);
            
            System.out.println("Sending email...");
            mailSender.send(message);
            System.out.println("Email sent successfully!");
            
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("MessagingException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error sending verification email: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error sending verification email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send new verification email
     */
    public void sendNewVerificationEmail(RegularUser user, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
            
            Context context = new Context();
            context.setVariable("userName", user.getName() + " " + user.getSurname());
            context.setVariable("appName", appName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("isResend", true);
            
            String htmlContent = templateEngine.process("email/verification", context);
            
            helper.setFrom(fromEmail, appName);
            helper.setTo(user.getEmail());
            helper.setSubject("New verification link - " + appName);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error sending new verification email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send welcome email
     */
    public void sendWelcomeEmail(RegularUser user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Context context = new Context();
            context.setVariable("userName", user.getName() + " " + user.getSurname());
            context.setVariable("appName", appName);
            context.setVariable("loginUrl", baseUrl + "/login");
            
            String htmlContent = templateEngine.process("email/welcome", context);
            
            helper.setFrom(fromEmail, appName);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to " + appName + "!");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (MessagingException | UnsupportedEncodingException e) {
            // We don't throw exception for welcome email because it's not critical
            System.err.println("Error sending welcome email: " + e.getMessage());
        }
    }
}