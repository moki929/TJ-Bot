package org.togetherjava.tjbot.jda;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.togetherjava.tjbot.commands.SlashCommand;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

/**
 * Builder to create button click events that can be used for example with
 * {@link SlashCommand#onButtonClick(ButtonInteractionEvent, List)}.
 * <p>
 * Create instances of this class by using {@link JdaTester#createButtonInteractionEvent()}.
 * <p>
 * Among other Discord related things, the builder optionally accepts a message
 * ({@link #setMessage(Message)}) and the user who clicked on the button
 * ({@link #setUserWhoClicked(Member)} ). As well as several ways to modify the message directly for
 * convenience, such as {@link #setContent(String)} or {@link #setActionRows(ActionRow...)}. The
 * builder is by default already setup with a valid dummy message and the user who clicked the
 * button is set to the author of the message.
 * <p>
 * In order to build the event, at least one button has to be added to the message and marked as
 * <i>clicked</i>. Therefore, use {@link #setActionRows(ActionRow...)} or modify the message
 * manually using {@link #setMessage(Message)}. Then mark the desired button as clicked using
 * {@link #build(Button)} or, if the message only contains a single button,
 * {@link #buildWithSingleButton()} will automatically select the button.
 * <p>
 * Refer to the following examples:
 *
 * <pre>
 * {@code
 * // Default message with a delete button
 * jdaTester.createButtonClickEvent()
 *   .setActionRows(ActionRow.of(Button.danger("1", "Delete"))
 *   .buildWithSingleButton();
 *
 * // More complex message with a user who clicked the button that is not the message author and multiple buttons
 * Button clickedButton = Button.primary("1", "Next");
 * jdaTester.createButtonClickEvent()
 *   .setMessage(new MessageBuilder()
 *     .setContent("See the following entry")
 *     .setEmbeds(
 *       new EmbedBuilder()
 *         .setDescription("John")
 *         .build())
 *     .build())
 *   .setUserWhoClicked(jdaTester.createMemberSpy(5))
 *   .setActionRows(
 *     ActionRow.of(Button.primary("1", "Previous"),
 *     clickedButton)
 *   .build(clickedButton);
 * }
 * </pre>
 */
public final class ButtonClickEventBuilder {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Supplier<? extends ButtonInteractionEvent> mockEventSupplier;
    private final UnaryOperator<Message> mockMessageOperator;
    private MessageBuilder messageBuilder;
    private Member userWhoClicked;

    ButtonClickEventBuilder(Supplier<? extends ButtonInteractionEvent> mockEventSupplier,
            UnaryOperator<Message> mockMessageOperator) {
        this.mockEventSupplier = mockEventSupplier;
        this.mockMessageOperator = mockMessageOperator;

        messageBuilder = new MessageBuilder();
        messageBuilder.setContent("test message");
    }

    /**
     * Sets the given message that this event is associated to. Will override any data previously
     * set with the more direct methods such as {@link #setContent(String)} or
     * {@link #setActionRows(ActionRow...)}.
     * <p>
     * The message must contain at least one button, or the button has to be added later with
     * {@link #setActionRows(ActionRow...)}.
     *
     * @param message the message to set
     * @return this builder instance for chaining
     */
    public ButtonClickEventBuilder setMessage(Message message) {
        messageBuilder = new MessageBuilder(message);
        return this;
    }

    /**
     * Sets the content of the message that this event is associated to. Usage of
     * {@link #setMessage(Message)} will overwrite any content set by this.
     * 
     * @param content the content of the message
     * @return this builder instance for chaining
     */
    public ButtonClickEventBuilder setContent(String content) {
        messageBuilder.setContent(content);
        return this;
    }

    /**
     * Sets the embeds of the message that this event is associated to. Usage of
     * {@link #setMessage(Message)} will overwrite any content set by this.
     * 
     * @param embeds the embeds of the message
     * @return this builder instance for chaining
     */
    public ButtonClickEventBuilder setEmbeds(MessageEmbed... embeds) {
        messageBuilder.setEmbeds(embeds);
        return this;
    }

    /**
     * Sets the action rows of the message that this event is associated to. Usage of
     * {@link #setMessage(Message)} will overwrite any content set by this.
     * <p>
     * At least one of the rows must contain a button before {@link #build(Button)} is called.
     * 
     * @param rows the action rows of the message
     * @return this builder instance for chaining
     */
    public ButtonClickEventBuilder setActionRows(ActionRow... rows) {
        messageBuilder.setActionRows(rows);
        return this;
    }

    /**
     * Sets the user who clicked the button, i.e. who triggered the event.
     * 
     * @param userWhoClicked the user who clicked the button
     * @return this builder instance for chaining
     */
    public ButtonClickEventBuilder setUserWhoClicked(Member userWhoClicked) {
        this.userWhoClicked = userWhoClicked;
        return this;
    }

    /**
     * Builds an instance of a button click event, corresponding to the current configuration of the
     * builder.
     * <p>
     * The message must contain exactly one button, which is automatically assumed to be the button
     * that has been clicked. Use {@link #build(Button)} for messages with multiple buttons instead.
     *
     * @return the created slash command instance
     */
    public ButtonInteractionEvent buildWithSingleButton() {
        return createEvent(null);
    }

    /**
     * Builds an instance of a button click event, corresponding to the current configuration of the
     * builder.
     * <p>
     * The message must the given button. {@link #buildWithSingleButton()} can be used for
     * convenience for messages that only have a single button.
     *
     * @param clickedButton the button that was clicked, i.e. that triggered the event. Must be
     *        contained in the message.
     * @return the created slash command instance
     */
    public ButtonInteractionEvent build(Button clickedButton) {
        return createEvent(clickedButton);
    }

    private ButtonInteractionEvent createEvent(@Nullable Button maybeClickedButton) {
        Message message = mockMessageOperator.apply(messageBuilder.build());
        Button clickedButton = determineClickedButton(maybeClickedButton, message);

        return mockButtonClickEvent(message, clickedButton);
    }

    private static Button determineClickedButton(@Nullable Button maybeClickedButton,
            Message message) {
        if (maybeClickedButton != null) {
            return requireButtonInMessage(maybeClickedButton, message);
        }

        // Otherwise, attempt to extract the button from the message. Only allow a single button in
        // this case to prevent ambiguity.
        return requireSingleButton(getMessageButtons(message));
    }

    private static Button requireButtonInMessage(Button clickedButton, Message message) {
        boolean isClickedButtonUnknown =
                getMessageButtons(message).noneMatch(clickedButton::equals);

        if (isClickedButtonUnknown) {
            throw new IllegalArgumentException(
                    "The given clicked button is not part of the messages components,"
                            + " make sure to add the button to one of the messages action rows first.");
        }
        return clickedButton;
    }

    private static Button requireSingleButton(Stream<? extends Button> stream) {
        Function<String, ? extends RuntimeException> descriptionToException =
                IllegalArgumentException::new;

        return stream.reduce((x, y) -> {
            throw descriptionToException
                .apply("The message contains more than a single button, unable to automatically determine the clicked button."
                        + " Either only use a single button or explicitly state the clicked button");
        })
            .orElseThrow(() -> descriptionToException.apply(
                    "The message contains no buttons, unable to automatically determine the clicked button."
                            + " Add the button to the message first."));
    }

    private static Stream<Button> getMessageButtons(Message message) {
        return message.getActionRows().stream().map(ActionRow::getButtons).flatMap(List::stream);
    }

    private ButtonInteractionEvent mockButtonClickEvent(Message message, Button clickedButton) {
        ButtonInteractionEvent event = mockEventSupplier.get();

        when(event.getMessage()).thenReturn(message);
        when(event.getButton()).thenReturn(clickedButton);
        when(event.getComponent()).thenReturn(clickedButton);
        when(event.getComponentId()).thenReturn(clickedButton.getId());
        when(event.getComponentType()).thenReturn(clickedButton.getType());

        when(event.getMember()).thenReturn(userWhoClicked);
        User asUser = userWhoClicked.getUser();
        when(event.getUser()).thenReturn(asUser);

        return event;
    }
}
