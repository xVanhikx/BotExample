package org.example.service;

import org.example.dto.MailParams;

public interface MailSenderService {
    public void send(MailParams mailParams);
}
