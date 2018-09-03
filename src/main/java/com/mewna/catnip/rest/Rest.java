package com.mewna.catnip.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mewna.catnip.internal.CatnipImpl;
import com.mewna.catnip.entity.*;
import com.mewna.catnip.rest.RestRequester.OutboundRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author amy
 * @since 9/1/18.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Rest {
    @Getter
    private final CatnipImpl catnip;
    
    public Rest(final CatnipImpl catnip) {
        this.catnip = catnip;
    }
    
    @Nonnull
    @CheckReturnValue
    private static <T> Function<JsonArray, List<T>> mapObjectContents(@Nonnull final Function<JsonObject, T> builder) {
        return array -> {
            final Collection<T> result = new ArrayList<>(array.size());
            for(final Object object : array) {
                if(!(object instanceof JsonObject)) {
                    throw new IllegalArgumentException("Expected array to contain only objects, but found " +
                            (object == null ? "null" : object.getClass())
                    );
                }
                result.add(builder.apply((JsonObject) object));
            }
            return ImmutableList.copyOf(result);
        };
    }
    
    @Nonnull
    public CompletableFuture<Message> createMessage(@Nonnull final String channelId, @Nonnull final String content) {
        return createMessage(channelId, new MessageBuilder().content(content).build());
    }
    
    @Nonnull
    public CompletableFuture<Message> createMessage(@Nonnull final String channelId, @Nonnull final Message message) {
        final JsonObject json = new JsonObject();
        if(message.content() != null && !message.content().isEmpty()) {
            json.put("content", message.content());
        }
        if(message.embeds() != null && !message.embeds().isEmpty()) {
            json.put("embeds", new JsonArray(message.embeds()));
        }
        if(json.getValue("embeds", null) == null && json.getValue("content", null) == null) {
            throw new IllegalArgumentException("Can't build a message with no content and no embeds!");
        }
        
        return catnip._requester().
                queue(new OutboundRequest(Routes.CREATE_MESSAGE.withMajorParam(channelId), ImmutableMap.of(), json))
                .thenApply(ResponsePayload::object)
                .thenApply(EntityBuilder::createMessage);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<Message> getMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return catnip._requester().queue(
                new OutboundRequest(Routes.GET_CHANNEL_MESSAGE.withMajorParam(channelId),
                        ImmutableMap.of("message.id", messageId), null))
                .thenApply(ResponsePayload::object)
                .thenApply(EntityBuilder::createMessage);
    }
    
    @Nonnull
    public CompletableFuture<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                  @Nonnull final String content) {
        return editMessage(channelId, messageId, new MessageBuilder().content(content).build());
    }
    
    @Nonnull
    public CompletableFuture<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                  @Nonnull final Message message) {
        final JsonObject json = new JsonObject();
        if(message.content() != null && !message.content().isEmpty()) {
            json.put("content", message.content());
        }
        if(message.embeds() != null && !message.embeds().isEmpty()) {
            json.put("embeds", new JsonArray(message.embeds()));
        }
        if(json.getValue("embeds", null) == null && json.getValue("content", null) == null) {
            throw new IllegalArgumentException("Can't build a message with no content and no embeds!");
        }
        return catnip._requester()
                .queue(new OutboundRequest(Routes.EDIT_MESSAGE.withMajorParam(channelId),
                        ImmutableMap.of("message.id", messageId), json))
                .thenApply(ResponsePayload::object)
                .thenApply(EntityBuilder::createMessage);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<User> getUser(@Nonnull final String userId) {
        return catnip._requester().queue(new OutboundRequest(Routes.GET_USER,
                ImmutableMap.of("user.id", userId), null))
                .thenApply(ResponsePayload::object)
                .thenApply(EntityBuilder::createUser);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletableFuture<List<Role>> getGuildRoles(@Nonnull final String guildId) {
        return catnip._requester()
                .queue(new OutboundRequest(Routes.GET_GUILD_ROLES.withMajorParam(guildId),
                        ImmutableMap.of(), null))
                .thenApply(ResponsePayload::array)
                .thenApply(mapObjectContents(EntityBuilder::createRole));
    }
}
