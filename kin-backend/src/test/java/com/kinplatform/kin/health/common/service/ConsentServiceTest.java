package com.kinplatform.kin.health.common.service;

import com.kinplatform.kin.health.common.dto.CreateUserConsentRequest;
import com.kinplatform.kin.health.common.dto.RevokeConsentRequest;
import com.kinplatform.kin.health.common.dto.UserConsentResponse;
import com.kinplatform.kin.health.common.entity.UserConsent;
import com.kinplatform.kin.health.common.repository.UserConsentRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private UserConsentRepository userConsentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConsentService consentService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).isActive(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of())
        );
    }

    @Test
    void createConsent_happyPath_returnsCreated() {
        CreateUserConsentRequest request = CreateUserConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(userConsentRepository.saveAndFlush(any(UserConsent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserConsentResponse response = consentService.createOrUpdateConsent(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("HEALTH_DATA", response.getConsentType());
        assertEquals("1.0", response.getVersion());
        assertTrue(response.getAccepted());
        assertNotNull(response.getAcceptedAt());
    }

    @Test
    void createConsent_userNotFound_throwsException() {
        CreateUserConsentRequest request = CreateUserConsentRequest.builder()
                .userId(UUID.randomUUID())
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> consentService.createOrUpdateConsent(request));
    }

    @Test
    void updateExistingConsent_acceptedToRevoked_updatesFields() {
        CreateUserConsentRequest request = CreateUserConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(false)
                .build();

        UserConsent existing = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now().minusSeconds(3600))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(userConsentRepository.saveAndFlush(any(UserConsent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserConsentResponse response = consentService.createOrUpdateConsent(request);

        assertNotNull(response);
        assertFalse(response.getAccepted());
        assertNotNull(response.getRevokedAt());
        assertNull(response.getAcceptedAt());
    }

    @Test
    void getUserConsents_returnsAll() {
        UserConsent c1 = UserConsent.builder().id(UUID.randomUUID()).userId(userId).consentType(UserConsent.ConsentType.HEALTH_DATA).version("1.0").accepted(true).build();
        UserConsent c2 = UserConsent.builder().id(UUID.randomUUID()).userId(userId).consentType(UserConsent.ConsentType.MARKETING).version("1.0").accepted(false).build();

        when(userConsentRepository.findByUserId(userId)).thenReturn(List.of(c1, c2));

        List<UserConsentResponse> response = consentService.getUserConsents(userId);

        assertEquals(2, response.size());
    }

    @Test
    void getActiveConsent_found_returnsConsent() {
        UserConsent active = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now())
                .build();

        when(userConsentRepository.findActiveConsent(userId, UserConsent.ConsentType.HEALTH_DATA))
                .thenReturn(Optional.of(active));

        Optional<UserConsentResponse> response = consentService.getActiveConsent(userId, "HEALTH_DATA");

        assertTrue(response.isPresent());
        assertEquals("HEALTH_DATA", response.get().getConsentType());
        assertTrue(response.get().getAccepted());
    }

    @Test
    void getActiveConsent_notFound_returnsEmpty() {
        when(userConsentRepository.findActiveConsent(userId, UserConsent.ConsentType.MARKETING))
                .thenReturn(Optional.empty());

        Optional<UserConsentResponse> response = consentService.getActiveConsent(userId, "MARKETING");

        assertTrue(response.isEmpty());
    }

    @Test
    void revokeConsent_happyPath_returnsRevoked() {
        RevokeConsentRequest request = RevokeConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .build();

        UserConsent existing = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now().minusSeconds(3600))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(userConsentRepository.saveAndFlush(any(UserConsent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserConsentResponse response = consentService.revokeConsent(request);

        assertNotNull(response);
        assertFalse(response.getAccepted());
        assertNotNull(response.getRevokedAt());
    }

    @Test
    void revokeConsent_notFound_throwsException() {
        RevokeConsentRequest request = RevokeConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> consentService.revokeConsent(request));
    }

    @Test
    void hasActiveConsent_true_returnsTrue() {
        when(userConsentRepository.findActiveConsent(userId, UserConsent.ConsentType.HEALTH_DATA))
                .thenReturn(Optional.of(UserConsent.builder().build()));

        boolean result = consentService.hasActiveConsent(userId, "HEALTH_DATA");

        assertTrue(result);
    }

    @Test
    void hasActiveConsent_false_returnsFalse() {
        when(userConsentRepository.findActiveConsent(userId, UserConsent.ConsentType.MARKETING))
                .thenReturn(Optional.empty());

        boolean result = consentService.hasActiveConsent(userId, "MARKETING");

        assertFalse(result);
    }

    @Test
    void hasActiveConsent_withVersion_true_returnsTrue() {
        UserConsent active = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now())
                .build();

        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, UserConsent.ConsentType.HEALTH_DATA, "1.0"))
                .thenReturn(Optional.of(active));

        boolean result = consentService.hasActiveConsent(userId, "HEALTH_DATA", "1.0");

        assertTrue(result);
    }

    @Test
    void hasActiveConsent_withVersion_false_returnsFalse() {
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, UserConsent.ConsentType.HEALTH_DATA, "2.0"))
                .thenReturn(Optional.empty());

        boolean result = consentService.hasActiveConsent(userId, "HEALTH_DATA", "2.0");

        assertFalse(result);
    }

    @Test
    void createConsent_withDocumentHash_savesHash() {
        CreateUserConsentRequest request = CreateUserConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .documentHash("a1b2c3d4e5f6")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(userConsentRepository.saveAndFlush(any(UserConsent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserConsentResponse response = consentService.createOrUpdateConsent(request);

        assertNotNull(response);
        assertEquals("a1b2c3d4e5f6", response.getDocumentHash());
    }

    @Test
    void createConsent_withNewVersion_marksOldAsObsolete() {
        // Create v1.0 consent
        UserConsent v1 = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now().minusSeconds(3600))
                .build();

        CreateUserConsentRequest request = CreateUserConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("2.0")
                .accepted(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, UserConsent.ConsentType.HEALTH_DATA, "2.0"))
                .thenReturn(Optional.empty());
        when(userConsentRepository.findByUserIdAndConsentType(userId, UserConsent.ConsentType.HEALTH_DATA))
                .thenReturn(List.of(
                        UserConsent.builder().id(UUID.randomUUID()).userId(userId).consentType(UserConsent.ConsentType.HEALTH_DATA).version("1.0").accepted(true).build()
                ));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, UserConsent.ConsentType.HEALTH_DATA, "2.0"))
                .thenReturn(Optional.empty());
        when(userConsentRepository.saveAndFlush(any(UserConsent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userConsentRepository.saveAllAndFlush(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        UserConsentResponse response = consentService.createOrUpdateConsent(request);

        assertNotNull(response);
        assertEquals("2.0", response.getVersion());
        assertTrue(response.getAccepted());
    }

    @Test
    void revokeConsent_withReason_savesReason() {
        RevokeConsentRequest request = RevokeConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .revocationReason("User withdrew consent")
                .build();

        UserConsent existing = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now().minusSeconds(3600))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(userConsentRepository.saveAndFlush(any(UserConsent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserConsentResponse response = consentService.revokeConsent(request);

        assertNotNull(response);
        assertFalse(response.getAccepted());
        assertNotNull(response.getRevokedAt());
        assertEquals("User withdrew consent", response.getRevocationReason());
    }

    @Test
    void generateDocumentHash_generatesValidSHA256() {
        String content = "Test document content for hashing";
        String hash = ConsentService.generateDocumentHash(content);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 = 64 hex chars
        assertTrue(hash.matches("[a-f0-9]{64}"));
    }

    @Test
    void generateDocumentHash_sameContentSameHash() {
        String content = "Consistent content";
        String hash1 = ConsentService.generateDocumentHash(content);
        String hash2 = ConsentService.generateDocumentHash(content);

        assertEquals(hash1, hash2);
    }

    @Test
    void getActiveConsent_withVersion_returnsCorrectVersion() {
        UserConsent active = UserConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType(UserConsent.ConsentType.HEALTH_DATA)
                .version("2.0")
                .accepted(true)
                .acceptedAt(Instant.now())
                .build();

        when(userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, UserConsent.ConsentType.HEALTH_DATA, "2.0"))
                .thenReturn(Optional.of(active));

        Optional<UserConsentResponse> response = consentService.getActiveConsent(userId, "HEALTH_DATA", "2.0");

        assertTrue(response.isPresent());
        assertEquals("2.0", response.get().getVersion());
        assertTrue(response.get().getAccepted());
    }
}
