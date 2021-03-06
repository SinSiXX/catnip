/*
 * Copyright (c) 2018 amy, All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mewna.catnip.rest.handler;

import com.google.common.collect.ImmutableMap;
import com.mewna.catnip.entity.builder.MessageBuilder;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.channel.GuildChannel;
import com.mewna.catnip.entity.channel.GuildChannel.ChannelEditFields;
import com.mewna.catnip.entity.channel.Webhook;
import com.mewna.catnip.entity.guild.PermissionOverride;
import com.mewna.catnip.entity.guild.PermissionOverride.OverrideType;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.misc.CreatedInvite;
import com.mewna.catnip.entity.misc.Emoji;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.util.Permission;
import com.mewna.catnip.internal.CatnipImpl;
import com.mewna.catnip.rest.ResponsePayload;
import com.mewna.catnip.rest.RestRequester.OutboundRequest;
import com.mewna.catnip.rest.Routes;
import com.mewna.catnip.rest.invite.InviteCreateOptions;
import com.mewna.catnip.util.pagination.MessagePaginator;
import com.mewna.catnip.util.pagination.ReactionPaginator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author amy
 * @since 9/3/18.
 */
@SuppressWarnings({"unused", "WeakerAccess", "ConstantConditions"})
public class RestChannel extends RestHandler {
    public RestChannel(final CatnipImpl catnip) {
        super(catnip);
    }
    
    @Nonnull
    public CompletionStage<Message> sendMessage(@Nonnull final String channelId, @Nonnull final String content) {
        return sendMessage(channelId, new MessageBuilder().content(content).build());
    }
    
    @Nonnull
    public CompletionStage<Message> sendMessage(@Nonnull final String channelId, @Nonnull final Embed embed) {
        return sendMessage(channelId, new MessageBuilder().embed(embed).build());
    }
    
    @Nonnull
    public CompletionStage<Message> sendMessage(@Nonnull final String channelId, @Nonnull final Message message) {
        final JsonObject json = new JsonObject();
        if(message.content() != null && !message.content().isEmpty()) {
            json.put("content", message.content());
        }
        if(message.embeds() != null && !message.embeds().isEmpty()) {
            json.put("embed", getEntityBuilder().embedToJson(message.embeds().get(0)));
        }
        if(json.getValue("embed", null) == null && json.getValue("content", null) == null) {
            throw new IllegalArgumentException("Can't build a message with no content and no embeds!");
        }
        
        return getCatnip().requester().
                queue(new OutboundRequest(Routes.CREATE_MESSAGE.withMajorParam(channelId), ImmutableMap.of(), json))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    public final CompletionStage<Message> sendMessage(@Nonnull final String channelId, @Nonnull final MessageOptions options) {
        final JsonObject json = new JsonObject();
        
        if(options.content() != null && !options.content().isEmpty()) {
            json.put("content", options.content());
        }
        
        if(options.embed() != null) {
            json.put("embed", getEntityBuilder().embedToJson(options.embed()));
        }
        
        if(json.getValue("embed", null) == null && json.getValue("content", null) == null && !options.hasFiles()) {
            throw new IllegalArgumentException("Can't build a message with no content, no embeds and no files!");
        }
        
        return getCatnip().requester().
                queue(new OutboundRequest(Routes.CREATE_MESSAGE.withMajorParam(channelId), ImmutableMap.of(), json).buffers(options.files()))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<Message> getMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(
                new OutboundRequest(Routes.GET_CHANNEL_MESSAGE.withMajorParam(channelId),
                        ImmutableMap.of("message.id", messageId)))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    public CompletionStage<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                @Nonnull final String content) {
        return editMessage(channelId, messageId, new MessageBuilder().content(content).build());
    }
    
    @Nonnull
    public CompletionStage<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                @Nonnull final Embed embed) {
        return editMessage(channelId, messageId, new MessageBuilder().embed(embed).build());
    }
    
    @Nonnull
    public CompletionStage<Message> editMessage(@Nonnull final String channelId, @Nonnull final String messageId,
                                                @Nonnull final Message message) {
        final JsonObject json = new JsonObject();
        if(message.embeds().isEmpty() && (message.content() == null || message.content().isEmpty())) {
            throw new IllegalArgumentException("Can't build a message with no content and no embed!");
        }
        json.put("content", message.content());
        if(message.embeds() != null && !message.embeds().isEmpty()) {
            json.put("embed", getEntityBuilder().embedToJson(message.embeds().get(0)));
        }
        if(json.getValue("embed", null) == null && json.getValue("content", null) == null) {
            throw new IllegalArgumentException("Can't build a message with no content and no embed!");
        }
        return getCatnip().requester()
                .queue(new OutboundRequest(Routes.EDIT_MESSAGE.withMajorParam(channelId),
                        ImmutableMap.of("message.id", messageId), json))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createMessage);
    }
    
    @Nonnull
    public CompletionStage<Void> deleteMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_MESSAGE.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId))).thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> deleteMessages(@Nonnull final String channelId, @Nonnull final List<String> messageIds) {
        return getCatnip().requester()
                .queue(new OutboundRequest(Routes.BULK_DELETE_MESSAGES.withMajorParam(channelId),
                        ImmutableMap.of(), new JsonObject().put("messages", new JsonArray(messageIds))))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> addReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                             @Nonnull final String emoji) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.CREATE_REACTION.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId, "emojis", encodeUTF8(emoji)), new JsonObject()))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> addReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                             @Nonnull final Emoji emoji) {
        return addReaction(channelId, messageId, emoji.forReaction());
    }
    
    @Nonnull
    public CompletionStage<Void> deleteOwnReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                                   @Nonnull final String emoji) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_OWN_REACTION.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId, "emojis", encodeUTF8(emoji))))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> deleteOwnReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                                   @Nonnull final Emoji emoji) {
        return deleteOwnReaction(channelId, messageId, emoji.forReaction());
    }
    
    @Nonnull
    public CompletionStage<Void> deleteUserReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                                   @Nonnull final String userId, @Nonnull final String emoji) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_USER_REACTION.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId, "emojis", encodeUTF8(emoji), "user.id", userId)))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> deleteUserReaction(@Nonnull final String channelId, @Nonnull final String messageId,
                                                   @Nonnull final String userId, @Nonnull final Emoji emoji) {
        return deleteUserReaction(channelId, messageId, userId, emoji.forReaction());
    }
    
    @Nonnull
    public CompletionStage<Void> deleteAllReactions(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_ALL_REACTIONS.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId)))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    @CheckReturnValue
    public ReactionPaginator getReactions(@Nonnull final String channelId, @Nonnull final String messageId,
                                          @Nonnull final String emoji) {
        return new ReactionPaginator(getEntityBuilder()) {
            @Nonnull
            @CheckReturnValue
            @Override
            protected CompletionStage<JsonArray> fetchNext(@Nonnull final RequestState<User> state, @Nullable final String lastId,
                                                           @Nonnegative final int requestSize) {
                return getReactionsRaw(channelId, messageId, emoji, null, lastId, requestSize);
            }
        };
    }
    
    @Nonnull
    @CheckReturnValue
    public ReactionPaginator getReactions(@Nonnull final String channelId, @Nonnull final String messageId,
                                          @Nonnull final Emoji emoji) {
        return getReactions(channelId, messageId, emoji.forReaction());
    }
    
    //TODO make public when we add raw methods for the other routes
    //keeping this private for consistency with the rest of the methods
    @Nonnull
    @CheckReturnValue
    private CompletionStage<JsonArray> getReactionsRaw(@Nonnull final String channelId, @Nonnull final String messageId,
                                                       @Nonnull final String emoji, @Nullable final String before,
                                                       @Nullable final String after, @Nonnegative final int limit) {
        final Collection<String> params = new ArrayList<>();
        if(limit > 0) {
            params.add("limit=" + limit);
        }
        if(before != null) {
            params.add("before=" + before);
        }
        if(after != null) {
            params.add("after=" + after);
        }
        String query = String.join("&", params);
        if(!query.isEmpty()) {
            query = '?' + query;
        }
        return getCatnip().requester()
                .queue(new OutboundRequest(Routes.GET_REACTIONS.withMajorParam(channelId).withQueryString(query),
                        ImmutableMap.of("message.id", messageId, "emojis", encodeUTF8(emoji))))
                .thenApply(ResponsePayload::array);
    }
    
    public CompletionStage<List<User>> getReactions(@Nonnull final String channelId, @Nonnull final String messageId,
                                                    @Nonnull final String emoji, @Nullable final String before,
                                                    @Nullable final String after, @Nonnegative final int limit) {
        return getReactionsRaw(channelId, messageId, emoji, before, after, limit)
                .thenApply(mapObjectContents(getEntityBuilder()::createUser))
                .thenApply(Collections::unmodifiableList);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<List<User>> getReactions(@Nonnull final String channelId, @Nonnull final String messageId,
                                                    @Nonnull final Emoji emoji, @Nullable final String before,
                                                    @Nullable final String after, @Nonnegative final int limit) {
        return getReactions(channelId, messageId, emoji.forReaction(), before, after, limit);
    }
    
    @Nonnull
    @CheckReturnValue
    public MessagePaginator getChannelMessages(@Nonnull final String channelId) {
        return new MessagePaginator(getEntityBuilder()) {
            @Nonnull
            @CheckReturnValue
            @Override
            protected CompletionStage<JsonArray> fetchNext(@Nonnull final RequestState<Message> state, @Nullable final String lastId,
                                                           @Nonnegative final int requestSize) {
                return getChannelMessagesRaw(channelId, lastId, null, null, requestSize);
            }
        };
    }
    
    //TODO make public when we add raw methods for the other routes
    //keeping this private for consistency with the rest of the methods
    @Nonnull
    @CheckReturnValue
    private CompletionStage<JsonArray> getChannelMessagesRaw(@Nonnull final String channelId, @Nullable final String before,
                                                             @Nullable final String after, @Nullable final String around,
                                                             @Nonnegative final int limit) {
        final Collection<String> params = new ArrayList<>();
        if(limit > 0) {
            params.add("limit=" + limit);
        }
        if(after != null) {
            params.add("after=" + after);
        }
        if(around != null) {
            params.add("around=" + around);
        }
        if(before != null) {
            params.add("before=" + before);
        }
        String query = String.join("&", params);
        if(!query.isEmpty()) {
            query = '?' + query;
        }
        return getCatnip().requester()
                .queue(new OutboundRequest(Routes.GET_CHANNEL_MESSAGES.withMajorParam(channelId).withQueryString(query),
                        ImmutableMap.of()))
                .thenApply(ResponsePayload::array);
    }
    
    public CompletionStage<List<Message>> getChannelMessages(@Nonnull final String channelId, @Nullable final String before,
                                                             @Nullable final String after, @Nullable final String around,
                                                             @Nonnegative final int limit) {
        return getChannelMessagesRaw(channelId, before, after, around, limit)
                .thenApply(mapObjectContents(getEntityBuilder()::createMessage))
                .thenApply(Collections::unmodifiableList);
    }
    
    @Nonnull
    public CompletionStage<Void> triggerTypingIndicator(@Nonnull final String channelId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.TRIGGER_TYPING_INDICATOR.withMajorParam(channelId),
                ImmutableMap.of(), new JsonObject()))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<Channel> getChannelById(@Nonnull final String channelId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.GET_CHANNEL.withMajorParam(channelId),
                ImmutableMap.of()))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createChannel);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<Channel> deleteChannel(@Nonnull final String channelId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_CHANNEL.withMajorParam(channelId),
                ImmutableMap.of()))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createChannel);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<CreatedInvite> createInvite(@Nonnull final String channelId,
                                                       @Nullable final InviteCreateOptions options) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.CREATE_CHANNEL_INVITE.withMajorParam(channelId),
                ImmutableMap.of(), (options == null ? InviteCreateOptions.create() : options).toJson()))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createCreatedInvite);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<List<CreatedInvite>> getChannelInvites(@Nonnull final String channelId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.GET_CHANNEL_INVITES.withMajorParam(channelId),
                ImmutableMap.of()))
                .thenApply(ResponsePayload::array)
                .thenApply(mapObjectContents(getEntityBuilder()::createCreatedInvite));
    }
    
    @Nonnull
    public CompletionStage<GuildChannel> modifyChannel(@Nonnull final String channelId,
                                                       @Nonnull final ChannelEditFields fields) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.MODIFY_CHANNEL.withMajorParam(channelId),
                ImmutableMap.of(), fields.payload()))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createGuildChannel);
    }
    
    @Nonnull
    public CompletionStage<Void> deletePermissionOverride(@Nonnull final String channelId, @Nonnull final String overwriteId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_CHANNEL_PERMISSION.withMajorParam(channelId),
                ImmutableMap.of("overwrite.id", overwriteId)))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> deletePermissionOverride(@Nonnull final String channelId,
                                                          @Nonnull final PermissionOverride overwrite) {
        return deletePermissionOverride(channelId, overwrite.id());
    }
    
    @Nonnull
    public CompletionStage<Void> editPermissionOverride(@Nonnull final String channelId, @Nonnull final String overwriteId,
                                                        @Nonnull final Collection<Permission> allowed,
                                                        @Nonnull final Collection<Permission> denied, final boolean isMember) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.EDIT_CHANNEL_PERMISSIONS.withMajorParam(channelId),
                ImmutableMap.of("overwrite.id", overwriteId), new JsonObject()
                .put("allow", Permission.from(allowed))
                .put("deny", Permission.from(denied))
                .put("type", isMember ? "member" : "role")))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> editPermissionOverride(@Nonnull final String channelId, @Nonnull final PermissionOverride overwrite,
                                                        @Nonnull final Collection<Permission> allowed,
                                                        @Nonnull final Collection<Permission> denied) {
        return editPermissionOverride(channelId, overwrite.id(), allowed, denied, overwrite.type() == OverrideType.MEMBER);
    }
    
    @Nonnull
    @CheckReturnValue
    public CompletionStage<List<Message>> getPinnedMessages(@Nonnull final String channelId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.GET_PINNED_MESSAGES.withMajorParam(channelId),
                ImmutableMap.of()))
                .thenApply(ResponsePayload::array)
                .thenApply(mapObjectContents(getEntityBuilder()::createMessage));
    }
    
    @Nonnull
    public CompletionStage<Void> deletePinnedMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.DELETE_PINNED_CHANNEL_MESSAGE.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId)))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> deletePinnedMessage(@Nonnull final Message message) {
        return deletePinnedMessage(message.channelId(), message.id());
    }
    
    @Nonnull
    public CompletionStage<Void> addPinnedMessage(@Nonnull final String channelId, @Nonnull final String messageId) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.ADD_PINNED_CHANNEL_MESSAGE.withMajorParam(channelId),
                ImmutableMap.of("message.id", messageId), new JsonObject()))
                .thenApply(__ -> null);
    }
    
    @Nonnull
    public CompletionStage<Void> addPinnedMessage(@Nonnull final Message message) {
        return addPinnedMessage(message.channelId(), message.id());
    }
    
    @Nonnull
    public CompletionStage<Webhook> createWebhook(@Nonnull final String channelId, @Nonnull final String name,
                                                  @Nullable final String avatar) {
        return getCatnip().requester().queue(new OutboundRequest(Routes.CREATE_WEBHOOK.withMajorParam(channelId),
                ImmutableMap.of(), new JsonObject().put("name", name).put("avatar", avatar)))
                .thenApply(ResponsePayload::object)
                .thenApply(getEntityBuilder()::createWebhook);
    }
}
