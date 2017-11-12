package com.ruskonert.GameServer.framework;

import com.ruskonert.GameServer.asm.TargetReference;
import com.ruskonert.GameServer.asm.TargetBuilder;
import com.ruskonert.GameServer.server.ConsoleSender;
import com.ruskonert.GameServer.MessageType;

import com.ruskonert.GameServer.server.Server;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ConsoleSenderFramework extends TargetBuilder<ConsoleSenderFramework> implements ConsoleSender
{
    @TargetReference(target="ProgramComponent", value="ConsoleMessageField")
    private TextField commandField;
    @Override public TextField getCommandField() { return this.commandField; }

    @TargetReference(target="ProgramComponent", value="ConsoleScreen")
    private TextArea consoleScreen;
    @Override public TextArea getConsoleScreen() { return this.consoleScreen; }

    @TargetReference(target="ProgramComponent", value="ConsoleMessageField#textProperty")
    private StringProperty messageProperty;
    public StringProperty getMessageProperty() { return this.messageProperty; }

    private Server server;
    public Server getServer() { return this.server; }

    protected ConsoleSenderFramework(Server server)
    {
        this.server = server;
    }

    @Override public final void sendMessage(@NotNull String message){ this.sendMessage(message, MessageType.INFO); }

    @Override public final void sendRawMessage(String message) { this.messageProperty.setValue(this.messageProperty.getValue() + message + "\n"); }

    @Override public void sendMessage(String message, MessageType type)
    {
        SimpleDateFormat sdf = this.getServer().getDateFormat();
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(sdf.format(new Date()));
        builder.append(" ");
        builder.append(type.getValue());
        builder.append("] ");
        builder.append(message);

        this.messageProperty.setValue(this.messageProperty.getValue() + builder.toString() + "\n");
    }

    @Override public final void clearScreen() { this.consoleScreen.setText(""); }

    @Override public final void clearCommandField() { this.commandField.setText("");}

    public void dispatch(String command, List<String> args)
    {
        //TODO("UP TO DATE");
    }

    public void messageDispatch(String message)
    {
        String command = this.commandField.getText();
        if(command.startsWith("/"))
        {
            command = command.substring(1, command.length() - 1);
            String[] split = command.split(" ");
            List<String> args = Arrays.asList(split);
            args.remove(0);
            this.dispatch(split[0], args);
        }
        else
        {
            this.sendMessage(message, MessageType.INFO);
        }
    }

    @Override
    public Object onInit(Object handleInstance)
    {
        super.onInit(this);
        return this;
    }
}