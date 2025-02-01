package org.example.service;

import org.example.utils.dto.MailParams;

public interface MailSenderService {
    public void send(MailParams mailParams);
}
