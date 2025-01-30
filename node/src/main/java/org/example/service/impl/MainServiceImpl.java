package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppUserDAO;
import org.example.dao.RawDataDAO;
import org.example.entity.*;
import org.example.exceptions.UploadFileException;
import org.example.service.FileService;
import org.example.service.MainService;
import org.example.service.ProducerService;
import org.example.service.TaskService;
import org.example.service.enums.ServiceCommand;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static org.example.entity.enums.UserState.BASIC_STATE;
import static org.example.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static org.example.service.enums.ServiceCommand.*;

@Service
@Log4j
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;
    private final TaskService taskService;
    private String lastCommand = "";

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDAO appUserDAO, FileService fileService, TaskService taskService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.fileService = fileService;
        this.taskService = taskService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);
        if (CANCEL.toString().equals(text)) {
            output = cancelProcess(appUser);
        } else if (lastCommand.equals(ADD.toString())) {
            output = addTask(appUser, text);
        } else if (lastCommand.equals(DELETE.toString())) {
            output = delete(appUser, text);
        } else if (lastCommand.equals(DONE.toString())) {
            output = completeTask(appUser, text);
        } else if (BASIC_STATE.equals(userState)) {
            output = processButtonCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            
        } else {
            log.error("Unknown user state: " + userState);
            output = "Неизвестная ошибка! Введите /cancel и попробуйте снова!";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);


    }

    private String processButtonCommand(AppUser appUser, String text) {
        switch (text) {
            case "Добавить":
                lastCommand = ADD.toString();
                return "Введите название задачи";
            case "Мои задачи":
                return getTasks(appUser);
            case "Завершить":
                lastCommand = DONE.toString();
                return "Введите название задачи для завершения";
            case "Удалить":
                lastCommand = DELETE.toString();
                return "Введите название задачи для удаления";
            default:
                return help();
        }
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try {
            AppDocument doc = fileService.processDoc(update.getMessage());

            var answer = "Документ успешно загружен! "
            + "Ссылка для скачивания: http://test.ru/get-doc/777";
            sendAnswer(answer, chatId);
        } catch (UploadFileException ex) {
            log.error(ex);
            String error = "К сожалению, загрузка файла не удалась. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        try {
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            var answer = "Фото успешно загружено! Ссылка для скачивания: http://test.ru/get-photo/777";
            sendAnswer(answer, chatId);
        } catch (UploadFileException ex) {
            log.error(ex);
            String error = "К сожалению, загрузка файла не удалась. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Зарегистрируйтесь или активируйте свою учетную запись для загрузки контента.";
            sendAnswer(error,chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Отмените текущую команду с помощью /cancel для отправки файлов.";
            sendAnswer(error,chatId);
            return true;
        }
        return false;
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        sendMessage.setReplyMarkup(createMainKeyboard());
        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        if (REGISTRATION.toString().equals(cmd)) {
            return "Временно недоступно!";
        } else if (HELP.toString().equals(cmd)) {
            return help();
        } else if (START.toString().equals(cmd)) {
            return "Приветствую! Используйте кнопки ниже для управления задачами, чтобы посмотреть список доступных команд введите /help!";
        } else if (cmd.startsWith("/add")) {
            return addTask(appUser, cmd);
        } else if (TASKS.toString().equals(cmd)) {
            return getTasks(appUser);
        } else if (cmd.startsWith("/done")) {
            return completeTask(appUser, cmd);
        } else if (cmd.startsWith("/delete")) {
            return delete(appUser, cmd);
        } else {
            return "Неизвестная команда! Чтобы посмотреть список доступных команд введите /help!";
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Добавить"));
        row1.add(new KeyboardButton("Мои задачи"));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Завершить"));
        row2.add(new KeyboardButton("Удалить"));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private String delete(AppUser appUser, String cmd) {
        String title;
        if (lastCommand.equals("")){
            String[] parts = cmd.split(" ", 2);
            if (parts.length < 2) {
                return "Неверный формат команды. Используйте: /delete <Название задачи>";
            }
            title = parts[1];
        } else {
            title = cmd;
        }
        taskService.removeTask(title);
        lastCommand = "";
        return "Задача удалена!";
    }

    private String completeTask(AppUser appUser, String cmd) {
        String title;
        if (lastCommand.equals("")){
            String[] parts = cmd.split(" ", 2);
            if (parts.length < 2) {
                return "Неверный формат команды. Используйте: /done <Название задачи>";
            }
            title = parts[1];
        } else {
            title = cmd;
        }
        if (title.matches("\\d+")) {
            taskService.markTaskAsCompletedWhenNotCompletedById(appUser, Long.parseLong(title));
        } else {
            taskService.markTaskAsCompletedWhenNotCompleted(appUser, title);
        }
        lastCommand = "";
        return "Задача выполнена!";
    }

    private String getTasks(AppUser appUser) {
        List<Task> tasks = taskService.getTasksByUser(appUser);
        if (tasks.isEmpty()) {
            return "У вас нет задач, наслаждайтесь жизнью!";
        }
        StringBuilder sb = new StringBuilder("Ваши задачи: \n");
        for (Task task : tasks) {
            sb.append(tasks.indexOf(task) + 1).append(". ").append(task.getTitle()).append(" - ").append(task.isCompleted()).append(".\n");
        }
        return (sb.toString());
    }

    private String addTask(AppUser appUser, String cmd) {
        String title;
        if (lastCommand.equals("")){
            String[] parts = cmd.split(" ", 2);
            if (parts.length < 2) {
                return "Неверный формат команды. Используйте: /add <название задачи> ";
            }
            title = parts[1];
        } else {
            title = cmd;
        }
        taskService.createTask(appUser, title);
        lastCommand = "";
        return "Задача добавлена " + title;
    }


    private String help() {
        return "Список доступных команд:\n" +
                "/cancel - отмена выполнения текущей команды;\n" +
                "/registration - регистрация пользователя.\n" +
                "/add <название> <описание> - добавление задачи\n" +
                "/tasks - список текущих задач\n" +
                "/delete <название> - удаление задачи\n" +
                "/done <название> - отметить задачу выполненной\n";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Команда отменена!";
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(telegramUser.getId());
        if (persistentAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return persistentAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }
}
