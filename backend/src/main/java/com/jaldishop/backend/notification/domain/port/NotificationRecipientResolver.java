package com.jaldishop.backend.notification.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientResolver {

    Optional<UUID> resolveMerchantUserId(UUID storeId);

}
