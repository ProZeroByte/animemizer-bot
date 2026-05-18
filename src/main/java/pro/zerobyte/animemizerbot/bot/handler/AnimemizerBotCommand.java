package pro.zerobyte.animemizerbot.bot.handler;

public enum AnimemizerBotCommand {
    START("/start", "Запустить бота и показать приветствие"),
    HELP("/help", "Показать список доступных команд"),
    STOP("/stop", "Выключить бота"),
    LIST("/list", "Показать список аниме"),
    BATCH("/batch", "Найти несколько аниме через запятую");

    private final String command;
    private final String description;

    AnimemizerBotCommand(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String getCommandName() {
        return command;
    }

    public String getDescription() {
        return description;
    }
}
