package com.yann.forcesub.service;

import com.yann.forcesub.entity.User;
import com.yann.forcesub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Test")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final Long TEST_USER_ID = 123456789L;
    private static final Long NON_EXISTENT_USER_ID = 999999999L;

    @BeforeEach
    void setUp() {
        testUser = new User(TEST_USER_ID);
    }

    @Nested
    @DisplayName("saveUser Tests")
    class SaveUserTests {

        @Test
        @DisplayName("Berhasil - Menyimpan user baru (user belum ada)")
        void saveUser_Success_WhenUserDoesNotExist() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(false));
            when(userRepository.insert(any(User.class))).thenReturn(Mono.just(testUser));

            // Act
            userService.saveUser(TEST_USER_ID);

            // Assert - Tunggu async operation selesai
            Thread.sleep(100);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
            verify(userRepository, times(1)).insert(userCaptor.capture());

            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser).isNotNull();
            assertThat(capturedUser.getId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Berhasil - Tidak menyimpan user yang sudah ada")
        void saveUser_Success_WhenUserAlreadyExists() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(true));

            // Act
            userService.saveUser(TEST_USER_ID);

            // Assert - Tunggu async operation selesai
            Thread.sleep(100);

            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("Gagal - Error saat mengecek existensi user")
        void saveUser_Fail_WhenExistsCheckThrowsError() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID))
                    .thenReturn(Mono.error(new RuntimeException("Database connection error")));

            // Act
            userService.saveUser(TEST_USER_ID);

            // Assert - Tunggu async operation selesai
            Thread.sleep(100);

            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("Gagal - Error saat menyimpan user")
        void saveUser_Fail_WhenInsertThrowsError() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(false));
            when(userRepository.insert(any(User.class)))
                    .thenReturn(Mono.error(new RuntimeException("Insert failed")));

            // Act
            userService.saveUser(TEST_USER_ID);

            // Assert - Tunggu async operation selesai
            Thread.sleep(100);

            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
            verify(userRepository, times(1)).insert(any(User.class));
        }

        @Test
        @DisplayName("Berhasil - Menyimpan multiple users secara berurutan")
        void saveUser_Success_SaveMultipleUsers() throws InterruptedException {
            // Arrange
            Long userId1 = 111L;
            Long userId2 = 222L;
            Long userId3 = 333L;

            when(userRepository.existsUserById(anyLong())).thenReturn(Mono.just(false));
            when(userRepository.insert(any(User.class)))
                    .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // Act
            userService.saveUser(userId1);
            userService.saveUser(userId2);
            userService.saveUser(userId3);

            // Assert - Tunggu async operation selesai
            Thread.sleep(200);

            verify(userRepository, times(3)).existsUserById(anyLong());
            verify(userRepository, times(3)).insert(any(User.class));
        }

        @Test
        @DisplayName("Berhasil - Tidak error dengan userId null")
        void saveUser_HandlesNull_WhenUserIdIsNull() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(null))
                    .thenReturn(Mono.error(new IllegalArgumentException("UserId cannot be null")));

            // Act
            userService.saveUser(null);

            // Assert - Tunggu async operation selesai
            Thread.sleep(100);

            verify(userRepository, times(1)).existsUserById(null);
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("Berhasil - Concurrent saves dengan user ID yang sama")
        void saveUser_Success_ConcurrentSavesWithSameId() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID))
                    .thenReturn(Mono.just(false))
                    .thenReturn(Mono.just(true));
            when(userRepository.insert(any(User.class))).thenReturn(Mono.just(testUser));

            // Act - Simulate concurrent saves
            userService.saveUser(TEST_USER_ID);
            userService.saveUser(TEST_USER_ID);

            // Assert
            Thread.sleep(200);

            // First call should save, second call should not
            verify(userRepository, times(2)).existsUserById(TEST_USER_ID);
            verify(userRepository, times(1)).insert(any(User.class));
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Berhasil - Menemukan user berdasarkan id")
        void findById_Success_WhenUserExists() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Mono.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNextMatches(user -> {
                        assertThat(user).isNotNull();
                        assertThat(user.getId()).isEqualTo(TEST_USER_ID);
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Berhasil - User tidak ditemukan (Empty Mono)")
        void findById_Success_WhenUserNotFound() {
            // Arrange
            when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(userService.findById(NON_EXISTENT_USER_ID))
                    .expectNextCount(0)
                    .verifyComplete();

            verify(userRepository, times(1)).findById(NON_EXISTENT_USER_ID);
        }

        @Test
        @DisplayName("Gagal - Error dari repository")
        void findById_Fail_WhenRepositoryThrowsError() {
            // Arrange
            RuntimeException expectedException = new RuntimeException("Database error");
            when(userRepository.findById(anyLong())).thenReturn(Mono.error(expectedException));

            // Act & Assert
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Database error"))
                    .verify();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Gagal - userId null")
        void findById_Fail_WhenUserIdIsNull() {
            // Arrange
            when(userRepository.findById((Long) null))
                    .thenReturn(Mono.error(new IllegalArgumentException("UserId cannot be null")));

            // Act & Assert
            StepVerifier.create(userService.findById(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        @Test
        @DisplayName("Berhasil - Timeout handling")
        void findById_Fail_WhenTimeout() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(Mono.just(testUser).delayElement(Duration.ofSeconds(5)));

            // Act & Assert
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectTimeout(Duration.ofSeconds(3))
                    .verify();
        }

        @Test
        @DisplayName("Berhasil - Multiple findById calls dengan caching behavior")
        void findById_Success_MultipleCalls() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Mono.just(testUser));

            // Act & Assert - First call
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNext(testUser)
                    .verifyComplete();

            // Act & Assert - Second call
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNext(testUser)
                    .verifyComplete();

            verify(userRepository, times(2)).findById(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("deleteById Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Berhasil - Menghapus user berdasarkan id")
        void deleteById_Success_WhenUserExists() {
            // Arrange
            when(userRepository.deleteById(TEST_USER_ID)).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .verifyComplete();

            verify(userRepository, times(1)).deleteById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Berhasil - Menghapus user yang tidak ada")
        void deleteById_Success_WhenUserNotFound() {
            // Arrange
            when(userRepository.deleteById(NON_EXISTENT_USER_ID)).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(userService.deleteById(NON_EXISTENT_USER_ID))
                    .verifyComplete();

            verify(userRepository, times(1)).deleteById(NON_EXISTENT_USER_ID);
        }

        @Test
        @DisplayName("Gagal - Error saat menghapus user")
        void deleteById_Fail_WhenRepositoryThrowsError() {
            // Arrange
            RuntimeException expectedException = new RuntimeException("Delete operation failed");
            when(userRepository.deleteById(anyLong())).thenReturn(Mono.error(expectedException));

            // Act & Assert
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Delete operation failed"))
                    .verify();

            verify(userRepository, times(1)).deleteById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Gagal - userId null")
        void deleteById_Fail_WhenUserIdIsNull() {
            // Arrange
            when(userRepository.deleteById((Long) null))
                    .thenReturn(Mono.error(new IllegalArgumentException("UserId cannot be null")));

            // Act & Assert
            StepVerifier.create(userService.deleteById(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        @Test
        @DisplayName("Berhasil - Delete after find")
        void deleteById_Success_AfterFind() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Mono.just(testUser));
            when(userRepository.deleteById(TEST_USER_ID)).thenReturn(Mono.empty());

            // Act & Assert - Find first
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNext(testUser)
                    .verifyComplete();

            // Act & Assert - Then delete
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .verifyComplete();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(userRepository, times(1)).deleteById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Berhasil - Multiple delete calls (idempotent)")
        void deleteById_Success_MultipleDeleteCalls() {
            // Arrange
            when(userRepository.deleteById(TEST_USER_ID)).thenReturn(Mono.empty());

            // Act & Assert - First delete
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .verifyComplete();

            // Act & Assert - Second delete
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .verifyComplete();

            verify(userRepository, times(2)).deleteById(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("count Tests")
    class CountTests {

        @Test
        @DisplayName("Berhasil - Menghitung total user (ada data)")
        void count_Success_WhenUsersExist() {
            // Arrange
            Long expectedCount = 10L;
            when(userRepository.count()).thenReturn(Mono.just(expectedCount));

            // Act & Assert
            StepVerifier.create(userService.count())
                    .expectNext(expectedCount)
                    .verifyComplete();

            verify(userRepository, times(1)).count();
        }

        @Test
        @DisplayName("Berhasil - Menghitung total user (tidak ada data)")
        void count_Success_WhenNoUsers() {
            // Arrange
            when(userRepository.count()).thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(userService.count())
                    .expectNext(0L)
                    .verifyComplete();

            verify(userRepository, times(1)).count();
        }

        @Test
        @DisplayName("Berhasil - Menghitung dengan jumlah besar")
        void count_Success_WithLargeCount() {
            // Arrange
            Long largeCount = 1_000_000L;
            when(userRepository.count()).thenReturn(Mono.just(largeCount));

            // Act & Assert
            StepVerifier.create(userService.count())
                    .expectNext(largeCount)
                    .verifyComplete();

            verify(userRepository, times(1)).count();
        }

        @Test
        @DisplayName("Gagal - Error dari repository")
        void count_Fail_WhenRepositoryThrowsError() {
            // Arrange
            RuntimeException expectedException = new RuntimeException("Count operation failed");
            when(userRepository.count()).thenReturn(Mono.error(expectedException));

            // Act & Assert
            StepVerifier.create(userService.count())
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Count operation failed"))
                    .verify();

            verify(userRepository, times(1)).count();
        }

        @Test
        @DisplayName("Gagal - Empty result")
        void count_Fail_WhenEmptyResult() {
            // Arrange
            when(userRepository.count()).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(userService.count())
                    .expectNextCount(0)
                    .verifyComplete();

            verify(userRepository, times(1)).count();
        }

        @Test
        @DisplayName("Berhasil - Count after multiple saves")
        void count_Success_AfterMultipleSaves() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(anyLong())).thenReturn(Mono.just(false));
            when(userRepository.insert(any(User.class)))
                    .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            when(userRepository.count()).thenReturn(Mono.just(3L));

            // Act - Save 3 users
            userService.saveUser(111L);
            userService.saveUser(222L);
            userService.saveUser(333L);
            Thread.sleep(200);

            // Act & Assert - Count
            StepVerifier.create(userService.count())
                    .expectNext(3L)
                    .verifyComplete();

            verify(userRepository, times(1)).count();
        }
    }

    @Nested
    @DisplayName("existsUserById Tests")
    class ExistsUserByIdTests {

        @Test
        @DisplayName("Berhasil - User ada (return true)")
        void existsUserById_Success_ReturnTrue_WhenUserExists() {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(true));

            // Act & Assert
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(true)
                    .verifyComplete();

            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Berhasil - User tidak ada (return false)")
        void existsUserById_Success_ReturnFalse_WhenUserNotFound() {
            // Arrange
            when(userRepository.existsUserById(NON_EXISTENT_USER_ID)).thenReturn(Mono.just(false));

            // Act & Assert
            StepVerifier.create(userService.existsUserById(NON_EXISTENT_USER_ID))
                    .expectNext(false)
                    .verifyComplete();

            verify(userRepository, times(1)).existsUserById(NON_EXISTENT_USER_ID);
        }

        @Test
        @DisplayName("Gagal - Error dari repository")
        void existsUserById_Fail_WhenRepositoryThrowsError() {
            // Arrange
            RuntimeException expectedException = new RuntimeException("Database error");
            when(userRepository.existsUserById(anyLong())).thenReturn(Mono.error(expectedException));

            // Act & Assert
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Database error"))
                    .verify();

            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Gagal - userId null")
        void existsUserById_Fail_WhenUserIdIsNull() {
            // Arrange
            when(userRepository.existsUserById(null))
                    .thenReturn(Mono.error(new IllegalArgumentException("UserId cannot be null")));

            // Act & Assert
            StepVerifier.create(userService.existsUserById(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        @Test
        @DisplayName("Berhasil - Multiple checks dengan hasil berbeda")
        void existsUserById_Success_MultipleChecks() {
            // Arrange
            Long existingUserId = 100L;
            Long nonExistingUserId = 200L;

            when(userRepository.existsUserById(existingUserId)).thenReturn(Mono.just(true));
            when(userRepository.existsUserById(nonExistingUserId)).thenReturn(Mono.just(false));

            // Act & Assert - User exists
            StepVerifier.create(userService.existsUserById(existingUserId))
                    .expectNext(true)
                    .verifyComplete();

            // Act & Assert - User doesn't exist
            StepVerifier.create(userService.existsUserById(nonExistingUserId))
                    .expectNext(false)
                    .verifyComplete();

            verify(userRepository, times(1)).existsUserById(existingUserId);
            verify(userRepository, times(1)).existsUserById(nonExistingUserId);
        }

        @Test
        @DisplayName("Berhasil - Check exists before and after save")
        void existsUserById_Success_BeforeAndAfterSave() throws InterruptedException {
            // Arrange
            when(userRepository.existsUserById(TEST_USER_ID))
                    .thenReturn(Mono.just(false))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(false)
                    .verifyComplete();

            // Act - Save user
            userService.saveUser(TEST_USER_ID);
            Thread.sleep(100);

            // Act & Assert - Check after save
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(true)
                    .verifyComplete();

            verify(userRepository, times(3)).existsUserById(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Integration-like Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Skenario lengkap - Save, Find, Exists, Count, Delete")
        void fullScenario_SaveFindExistsCountDelete() throws InterruptedException {
            // 1. Check user doesn't exist (pertama kali)
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(false));
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(false)
                    .verifyComplete();

            // 2. Save user (ini akan memanggil existsUserById lagi secara internal)
            when(userRepository.insert(any(User.class))).thenReturn(Mono.just(testUser));
            userService.saveUser(TEST_USER_ID);
            Thread.sleep(100);

            // 3. Check user now exists (ketiga kali)
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(true));
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(true)
                    .verifyComplete();

            // 4. Find user
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Mono.just(testUser));
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNext(testUser)
                    .verifyComplete();

            // 5. Count users
            when(userRepository.count()).thenReturn(Mono.just(1L));
            StepVerifier.create(userService.count())
                    .expectNext(1L)
                    .verifyComplete();

            // 6. Delete user
            when(userRepository.deleteById(TEST_USER_ID)).thenReturn(Mono.empty());
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .verifyComplete();

            // Verify all interactions
            // existsUserById dipanggil 3 kali:
            // 1. Manual check pertama
            // 2. Internal call dari saveUser
            // 3. Manual check kedua
            verify(userRepository, times(3)).existsUserById(TEST_USER_ID);
            verify(userRepository, times(1)).insert(any(User.class));
            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(userRepository, times(1)).count();
            verify(userRepository, times(1)).deleteById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Skenario - Tidak save user yang sudah ada")
        void scenario_DoNotSaveDuplicateUser() throws InterruptedException {
            // 1. User already exists
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(true));

            // 2. Try to save user
            userService.saveUser(TEST_USER_ID);
            Thread.sleep(100);

            // 3. Verify insert was not called
            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
            verify(userRepository, never()).insert(any(User.class));
        }

        @Test
        @DisplayName("Skenario - Save multiple users dan count")
        void scenario_SaveMultipleUsersAndCount() throws InterruptedException {
            // Arrange
            Long userId1 = 100L;
            Long userId2 = 200L;
            Long userId3 = 300L;

            when(userRepository.existsUserById(anyLong())).thenReturn(Mono.just(false));
            when(userRepository.insert(any(User.class)))
                    .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            when(userRepository.count()).thenReturn(Mono.just(3L));

            // Act - Save 3 users
            userService.saveUser(userId1);
            userService.saveUser(userId2);
            userService.saveUser(userId3);
            Thread.sleep(200);

            // Act & Assert - Count
            StepVerifier.create(userService.count())
                    .expectNext(3L)
                    .verifyComplete();

            verify(userRepository, times(3)).existsUserById(anyLong());
            verify(userRepository, times(3)).insert(any(User.class));
            verify(userRepository, times(1)).count();
        }

        @Test
        @DisplayName("Skenario - Find, Delete, Verify deleted")
        void scenario_FindDeleteVerify() {
            // 1. Find user
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Mono.just(testUser));
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNext(testUser)
                    .verifyComplete();

            // 2. Check exists before delete
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(true));
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(true)
                    .verifyComplete();

            when(userRepository.deleteById(TEST_USER_ID)).thenReturn(Mono.empty());
            StepVerifier.create(userService.deleteById(TEST_USER_ID))
                    .verifyComplete();

            // 4. Verify user deleted
            when(userRepository.existsUserById(TEST_USER_ID)).thenReturn(Mono.just(false));
            StepVerifier.create(userService.existsUserById(TEST_USER_ID))
                    .expectNext(false)
                    .verifyComplete();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(userRepository, times(2)).existsUserById(TEST_USER_ID);
            verify(userRepository, times(1)).deleteById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Skenario - Error handling chain")
        void scenario_ErrorHandlingChain() throws InterruptedException {
            when(userRepository.existsUserById(TEST_USER_ID))
                    .thenReturn(Mono.error(new RuntimeException("Connection lost")));

            userService.saveUser(TEST_USER_ID);
            Thread.sleep(100);

            // 2. Try to find - should still work independently
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Mono.just(testUser));
            StepVerifier.create(userService.findById(TEST_USER_ID))
                    .expectNext(testUser)
                    .verifyComplete();

            verify(userRepository, times(1)).existsUserById(TEST_USER_ID);
            verify(userRepository, never()).insert(any(User.class));
            verify(userRepository, times(1)).findById(TEST_USER_ID);
        }
    }
}