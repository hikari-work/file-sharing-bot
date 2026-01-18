package com.yann.forcesub.service;

import com.yann.forcesub.entity.Channel;
import com.yann.forcesub.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Channel Service Test")
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private ChannelService channelService;

    private Channel testChannel;
    private Long testId;
    private String testChannelLinks;
    private boolean testIsActive;

    @BeforeEach
    void setUp() {
        testId = 1L;
        testChannelLinks = "https://t.me/testchannel";
        testIsActive = true;

        testChannel = new Channel();
        testChannel.setId(testId);
        testChannel.setChannelLinks(testChannelLinks);
        testChannel.setActive(testIsActive);
    }

    @Test
    @DisplayName("Should save channel successfully")
    void shouldSaveChannelSuccessfully() {
        // Given
        when(channelRepository.save(any(Channel.class)))
                .thenReturn(Mono.just(testChannel));

        // When
        Mono<Channel> result = channelService.saveChannel(testId, testChannelLinks, testIsActive);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(channel ->
                        channel.getId().equals(testId) &&
                                channel.getChannelLinks().equals(testChannelLinks) &&
                                channel.isActive() == testIsActive
                )
                .verifyComplete();

        verify(channelRepository, times(1)).save(any(Channel.class));
    }

    @Test
    @DisplayName("Should save channel with inactive status")
    void shouldSaveChannelWithInactiveStatus() {
        // Given
        Channel inactiveChannel = new Channel();
        inactiveChannel.setId(testId);
        inactiveChannel.setChannelLinks(testChannelLinks);
        inactiveChannel.setActive(false);

        when(channelRepository.save(any(Channel.class)))
                .thenReturn(Mono.just(inactiveChannel));

        // When
        Mono<Channel> result = channelService.saveChannel(testId, testChannelLinks, false);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(channel -> !channel.isActive())
                .verifyComplete();

        verify(channelRepository, times(1)).save(any(Channel.class));
    }

    @Test
    @DisplayName("Should handle error when saving channel fails")
    void shouldHandleErrorWhenSavingChannelFails() {
        // Given
        when(channelRepository.save(any(Channel.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When
        Mono<Channel> result = channelService.saveChannel(testId, testChannelLinks, testIsActive);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error")
                )
                .verify();

        verify(channelRepository, times(1)).save(any(Channel.class));
    }

    @Test
    @DisplayName("Should find channel by id successfully")
    void shouldFindChannelByIdSuccessfully() {
        // Given
        when(channelRepository.findById(testId))
                .thenReturn(Mono.just(testChannel));

        // When
        Mono<Channel> result = channelService.findById(testId);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(channel ->
                        channel.getId().equals(testId) &&
                                channel.getChannelLinks().equals(testChannelLinks)
                )
                .verifyComplete();

        verify(channelRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should return empty when channel not found by id")
    void shouldReturnEmptyWhenChannelNotFoundById() {
        // Given
        when(channelRepository.findById(anyLong()))
                .thenReturn(Mono.empty());

        // When
        Mono<Channel> result = channelService.findById(999L);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(channelRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should handle error when finding channel by id fails")
    void shouldHandleErrorWhenFindingChannelByIdFails() {
        // Given
        when(channelRepository.findById(anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When
        Mono<Channel> result = channelService.findById(testId);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(channelRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should delete channel by id successfully")
    void shouldDeleteChannelByIdSuccessfully() {
        // Given
        when(channelRepository.deleteById(testId))
                .thenReturn(Mono.empty());

        // When
        Mono<Void> result = channelService.deleteById(testId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(channelRepository, times(1)).deleteById(testId);
    }

    @Test
    @DisplayName("Should handle error when deleting channel fails")
    void shouldHandleErrorWhenDeletingChannelFails() {
        // Given
        when(channelRepository.deleteById(anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        // When
        Mono<Void> result = channelService.deleteById(testId);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(channelRepository, times(1)).deleteById(testId);
    }

    @Test
    @DisplayName("Should count channels successfully")
    void shouldCountChannelsSuccessfully() {
        // Given
        Long expectedCount = 5L;
        when(channelRepository.count())
                .thenReturn(Mono.just(expectedCount));

        // When
        Mono<Long> result = channelService.count();

        // Then
        StepVerifier.create(result)
                .expectNext(expectedCount)
                .verifyComplete();

        verify(channelRepository, times(1)).count();
    }

    @Test
    @DisplayName("Should return zero when no channels exist")
    void shouldReturnZeroWhenNoChannelsExist() {
        // Given
        when(channelRepository.count())
                .thenReturn(Mono.just(0L));

        // When
        Mono<Long> result = channelService.count();

        // Then
        StepVerifier.create(result)
                .expectNext(0L)
                .verifyComplete();

        verify(channelRepository, times(1)).count();
    }

    @Test
    @DisplayName("Should handle error when counting channels fails")
    void shouldHandleErrorWhenCountingChannelsFails() {
        // Given
        when(channelRepository.count())
                .thenReturn(Mono.error(new RuntimeException("Count failed")));

        // When
        Mono<Long> result = channelService.count();

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(channelRepository, times(1)).count();
    }

    @Test
    @DisplayName("Should return true when channel exists by id")
    void shouldReturnTrueWhenChannelExistsById() {
        // Given
        when(channelRepository.existsById(testId))
                .thenReturn(Mono.just(true));

        // When
        Mono<Boolean> result = channelService.existsChannelById(testId);

        // Then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(channelRepository, times(1)).existsById(testId);
    }

    @Test
    @DisplayName("Should return false when channel does not exist by id")
    void shouldReturnFalseWhenChannelDoesNotExistById() {
        // Given
        when(channelRepository.existsById(anyLong()))
                .thenReturn(Mono.just(false));

        // When
        Mono<Boolean> result = channelService.existsChannelById(999L);

        // Then
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(channelRepository, times(1)).existsById(999L);
    }

    @Test
    @DisplayName("Should handle error when checking channel existence fails")
    void shouldHandleErrorWhenCheckingChannelExistenceFails() {
        // Given
        when(channelRepository.existsById(anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Exists check failed")));

        // When
        Mono<Boolean> result = channelService.existsChannelById(testId);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(channelRepository, times(1)).existsById(testId);
    }

    @Test
    @DisplayName("Should find all channels successfully")
    void shouldFindAllChannelsSuccessfully() {
        // Given
        Channel channel1 = new Channel();
        channel1.setId(1L);
        channel1.setChannelLinks("https://t.me/channel1");
        channel1.setActive(true);

        Channel channel2 = new Channel();
        channel2.setId(2L);
        channel2.setChannelLinks("https://t.me/channel2");
        channel2.setActive(false);

        Channel channel3 = new Channel();
        channel3.setId(3L);
        channel3.setChannelLinks("https://t.me/channel3");
        channel3.setActive(true);

        when(channelRepository.findAll())
                .thenReturn(Flux.just(channel1, channel2, channel3));

        // When
        Flux<Channel> result = channelService.findAll();

        // Then
        StepVerifier.create(result)
                .expectNext(channel1)
                .expectNext(channel2)
                .expectNext(channel3)
                .verifyComplete();

        verify(channelRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty flux when no channels exist")
    void shouldReturnEmptyFluxWhenNoChannelsExist() {
        // Given
        when(channelRepository.findAll())
                .thenReturn(Flux.empty());

        // When
        Flux<Channel> result = channelService.findAll();

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(channelRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle error when finding all channels fails")
    void shouldHandleErrorWhenFindingAllChannelsFails() {
        // Given
        when(channelRepository.findAll())
                .thenReturn(Flux.error(new RuntimeException("Find all failed")));

        // When
        Flux<Channel> result = channelService.findAll();

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(channelRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find all active channels successfully")
    void shouldFindAllActiveChannelsSuccessfully() {
        // Given
        Channel activeChannel1 = new Channel();
        activeChannel1.setId(1L);
        activeChannel1.setChannelLinks("https://t.me/active1");
        activeChannel1.setActive(true);

        Channel activeChannel2 = new Channel();
        activeChannel2.setId(2L);
        activeChannel2.setChannelLinks("https://t.me/active2");
        activeChannel2.setActive(true);

        when(channelRepository.findAllByIsActiveIs(true))
                .thenReturn(Flux.just(activeChannel1, activeChannel2));

        // When
        Flux<Channel> result = channelService.findAllActive(true);

        // Then
        StepVerifier.create(result)
                .expectNext(activeChannel1)
                .expectNext(activeChannel2)
                .verifyComplete();

        verify(channelRepository, times(1)).findAllByIsActiveIs(true);
    }

    @Test
    @DisplayName("Should find all inactive channels successfully")
    void shouldFindAllInactiveChannelsSuccessfully() {
        // Given
        Channel inactiveChannel1 = new Channel();
        inactiveChannel1.setId(1L);
        inactiveChannel1.setChannelLinks("https://t.me/inactive1");
        inactiveChannel1.setActive(false);

        Channel inactiveChannel2 = new Channel();
        inactiveChannel2.setId(2L);
        inactiveChannel2.setChannelLinks("https://t.me/inactive2");
        inactiveChannel2.setActive(false);

        when(channelRepository.findAllByIsActiveIs(false))
                .thenReturn(Flux.just(inactiveChannel1, inactiveChannel2));

        // When
        Flux<Channel> result = channelService.findAllActive(false);

        // Then
        StepVerifier.create(result)
                .expectNext(inactiveChannel1)
                .expectNext(inactiveChannel2)
                .verifyComplete();

        verify(channelRepository, times(1)).findAllByIsActiveIs(false);
    }

    @Test
    @DisplayName("Should return empty flux when no active channels exist")
    void shouldReturnEmptyFluxWhenNoActiveChannelsExist() {
        // Given
        when(channelRepository.findAllByIsActiveIs(anyBoolean()))
                .thenReturn(Flux.empty());

        // When
        Flux<Channel> result = channelService.findAllActive(true);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(channelRepository, times(1)).findAllByIsActiveIs(true);
    }

    @Test
    @DisplayName("Should handle error when finding active channels fails")
    void shouldHandleErrorWhenFindingActiveChannelsFails() {
        // Given
        when(channelRepository.findAllByIsActiveIs(anyBoolean()))
                .thenReturn(Flux.error(new RuntimeException("Find active failed")));

        // When
        Flux<Channel> result = channelService.findAllActive(true);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(channelRepository, times(1)).findAllByIsActiveIs(true);
    }

    @Test
    @DisplayName("Should verify all active channels returned are actually active")
    void shouldVerifyAllActiveChannelsReturnedAreActuallyActive() {
        // Given
        Channel channel1 = new Channel();
        channel1.setId(1L);
        channel1.setChannelLinks("https://t.me/channel1");
        channel1.setActive(true);

        Channel channel2 = new Channel();
        channel2.setId(2L);
        channel2.setChannelLinks("https://t.me/channel2");
        channel2.setActive(true);

        when(channelRepository.findAllByIsActiveIs(true))
                .thenReturn(Flux.just(channel1, channel2));

        // When
        Flux<Channel> result = channelService.findAllActive(true);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(Channel::isActive)
                .expectNextMatches(Channel::isActive)
                .verifyComplete();

        verify(channelRepository, times(1)).findAllByIsActiveIs(true);
    }

    @Test
    @DisplayName("Should handle null channel links when saving")
    void shouldHandleNullChannelLinksWhenSaving() {
        // Given
        Channel channelWithNullLinks = new Channel();
        channelWithNullLinks.setId(testId);
        channelWithNullLinks.setChannelLinks(null);
        channelWithNullLinks.setActive(true);

        when(channelRepository.save(any(Channel.class)))
                .thenReturn(Mono.just(channelWithNullLinks));

        // When
        Mono<Channel> result = channelService.saveChannel(testId, null, true);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(channel -> channel.getChannelLinks() == null)
                .verifyComplete();

        verify(channelRepository, times(1)).save(any(Channel.class));
    }

    @Test
    @DisplayName("Should handle empty string channel links when saving")
    void shouldHandleEmptyStringChannelLinksWhenSaving() {
        // Given
        Channel channelWithEmptyLinks = new Channel();
        channelWithEmptyLinks.setId(testId);
        channelWithEmptyLinks.setChannelLinks("");
        channelWithEmptyLinks.setActive(true);

        when(channelRepository.save(any(Channel.class)))
                .thenReturn(Mono.just(channelWithEmptyLinks));

        // When
        Mono<Channel> result = channelService.saveChannel(testId, "", true);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(channel ->
                        channel.getChannelLinks() != null &&
                                channel.getChannelLinks().isEmpty()
                )
                .verifyComplete();

        verify(channelRepository, times(1)).save(any(Channel.class));
    }
}