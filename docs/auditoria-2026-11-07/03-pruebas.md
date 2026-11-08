# Pruebas localizadas: evidencia estática

No se ejecutó ninguna prueba. Los nombres describen intención; no demuestran aserciones completas ni aprobación. Integración clasificada por anotaciones, nombre o dependencia PostgreSQL; revisar fixtures antes de autorizar ejecución.

## BackendApplicationTests.java

## address

### backend/src/test/java/com/jaldishop/backend/address/application/service/CreateCustomerAddressServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateFirstAddressAsDefaultAutomatically`, `shouldThrowWhenLimitExceeded`, `shouldThrowWhenUbigeoIsInvalid`

### backend/src/test/java/com/jaldishop/backend/address/application/service/DeleteCustomerAddressServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldDeleteDefaultAndPromoteNext`, `shouldThrowNotFoundWhenNotOwned`

### backend/src/test/java/com/jaldishop/backend/address/application/service/SetDefaultCustomerAddressServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldSetDefaultAddressSuccessfully`, `shouldThrowNotFoundWhenNotOwned`

### backend/src/test/java/com/jaldishop/backend/address/application/service/UpdateCustomerAddressServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateAddressSuccessfully`, `shouldThrowNotFoundWhenAddressBelongsToAnotherUser`

### backend/src/test/java/com/jaldishop/backend/address/infrastructure/ubigeo/UbigeoCatalogServiceTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `shouldLoadOfficialCatalogSuccessfully`, `shouldValidateLegitimateCodes`, `shouldRejectInvalidCodes`, `shouldRejectHierarchyMismatch`

### backend/src/test/java/com/jaldishop/backend/address/web/controller/CustomerAddressControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldGetAddresses`, `shouldCreateAddress`, `shouldReturn422WhenLimitExceeded`, `shouldSetDefault`, `shouldDeleteAddress`

## capacity

### backend/src/test/java/com/jaldishop/backend/capacity/application/availability/service/GetCapacityAvailabilityServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `availableCapacityFormula`, `availableCapacityClampsToZero`, `noCoverageReturnsZero`

### backend/src/test/java/com/jaldishop/backend/capacity/application/availability/service/GetEffectiveCapacityServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `appliesBaseSlot`, `appliesBaseFullDay`, `exceptionOverridesBase`, `exceptionCapacityZero`, `noConfigNoException`, `franjaFueraDeHorario`, `slotExceptionBeatsFullDayException`, `slotExceptionBeatsBase`, `inactiveConfigIgnored`, `inactiveExceptionIgnored`, `differentDayConfigNotApplied`, `baseSlotBeatsBaseFullDay`, `invalidTimeRange`, `missingFranja`, `missingDate`, `stubNoExceptions`, `stubExceptions`, `stubConfigs`

### backend/src/test/java/com/jaldishop/backend/capacity/application/availability/service/ListStoreCapacitySlotsServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `listsDayConfigurationsSorted`, `configsOfOtherDayIgnored`, `inactiveConfigsIgnored`, `fullDayExceptionAppliesAsReplacement`, `fullDayExceptionWithoutBaseCreatesNoSlot`, `slotExceptionListedAsOwnWindow`, `identicalWindowDeduplicated`, `inactiveSlotExceptionNotListed`, `fullDayConfigMaterializedAsSingleWindow`, `sundayUsesZeroBasedConvention`, `countsFeedAvailabilityFormula`, `noCandidatesReturnsEmptyList`, `pastDateRejected`, `missingArgumentsRejected`, `stubConfigs`, `stubExceptions`, `stubExceptionsFor`, `stubCounts`, `stubCountsFor`

### backend/src/test/java/com/jaldishop/backend/capacity/application/configuration/service/CreateCapacityConfigurationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createFullDaySuccessfully`, `createTimeSlotSuccessfully`, `throwExceptionWhenOverlap`, `noOverlapCheckForFullDay`

### backend/src/test/java/com/jaldishop/backend/capacity/application/configuration/service/CreateCapacityExceptionServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createFullDaySuccessfully`, `createTimeSlotSuccessfully`

### backend/src/test/java/com/jaldishop/backend/capacity/application/configuration/service/UpdateCapacityExceptionServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `updateSuccessfully`, `throwExceptionWhenNotFound`, `throwExceptionWhenNotFromStore`

### backend/src/test/java/com/jaldishop/backend/capacity/application/reservation/service/CreateCapacityReservationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createFromExceptionSuccessfully`, `createFromBaseSuccessfully`, `noCapacityRejected`, `lastSlotAvailable`, `exhaustedSlotRejected`, `duplicateActiveReservationRejected`, `missingAnchorRejected`, `stubResolution`

### backend/src/test/java/com/jaldishop/backend/capacity/application/reservation/service/GetCapacityReservationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `getOwnReservation`, `missingReservationThrows`, `foreignReservationThrows`, `getExpiredReservationPersistsExpiry`

### backend/src/test/java/com/jaldishop/backend/capacity/application/reservation/service/ReleaseCapacityReservationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `releaseActiveReservation`, `releaseProtectedReservation`, `committedReservationRejected`, `missingReservationThrows`, `foreignReservationThrows`, `releasedReservationIdempotent`

### backend/src/test/java/com/jaldishop/backend/capacity/application/reservation/service/ReleaseCommittedCapacityReservationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReleaseCommittedCapacityReservation`, `shouldBeIdempotentWhenAlreadyReleased`, `shouldDoNothingWhenReservationIdIsNull`

### backend/src/test/java/com/jaldishop/backend/capacity/domain/model/CapacityConfigurationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createFullDay`, `createTimeSlot`, `throwExceptionWhenStoreIdIsNull`, `throwExceptionWhenDayOfWeekIsNegative`, `throwExceptionWhenDayOfWeekExceeds6`, `throwExceptionWhenMaxCapacityIsNegative`, `allowMaxCapacityZero`, `throwExceptionWhenStartTimeWithoutEndTime`, `throwExceptionWhenEndTimeWithoutStartTime`, `throwExceptionWhenStartTimeNotBeforeEndTime`, `throwExceptionWhenStartTimeEqualsEndTime`, `activateAndDeactivate`, `updateConfiguration`, `reconstituteConfiguration`, `allDaysOfWeekAreValid`, `appliesToFullDayMatchesAnySlot`, `appliesToRejectsDifferentDay`, `appliesToSlotContainsRequest`, `appliesToSlotBoundaryStartEqual`, `appliesToSlotBoundaryEndEqual`, `appliesToSlotRequestBeforeStart`, `appliesToSlotRequestAfterEnd`, `appliesToSlotRequestCrosses`, `appliesToInactiveConfiguration`, `appliesToSlotDoesNotMatchFullDayRequest`

### backend/src/test/java/com/jaldishop/backend/capacity/domain/model/CapacityExceptionTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createFullDay`, `createTimeSlot`, `createWithoutReason`, `throwExceptionWhenStoreIdIsNull`, `throwExceptionWhenServiceDateIsNull`, `throwExceptionWhenCapacityIsNegative`, `allowCapacityZero`, `throwExceptionWhenStartTimeWithoutEndTime`, `throwExceptionWhenEndTimeWithoutStartTime`, `throwExceptionWhenStartTimeNotBeforeEndTime`, `throwExceptionWhenStartTimeEqualsEndTime`, `activateAndDeactivate`, `updateException`, `reconstituteException`, `appliesToFullDayMatchesAnySlot`, `appliesToSlotContainsRequest`, `appliesToSlotRequestBeforeStart`, `appliesToSlotRequestAfterEnd`, `appliesToSlotRequestCrosses`, `appliesToSlotDoesNotMatchFullDayRequest`, `appliesToInactiveException`

### backend/src/test/java/com/jaldishop/backend/capacity/domain/model/CapacityPeriodTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateAllDayPeriod`, `shouldCreateValidTimeSlotPeriod`, `shouldThrowWhenStartTimeWithoutEndTime`, `shouldThrowWhenEndTimeWithoutStartTime`, `shouldThrowWhenStartTimeNotBeforeEndTime`, `shouldThrowWhenStartTimeEqualsEndTime`, `shouldCheckContainmentCorrectly`

### backend/src/test/java/com/jaldishop/backend/capacity/domain/model/CapacityReservationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createActiveReservation`, `throwExceptionWhenStoreIdIsNull`, `throwExceptionWhenUserIdIsNull`, `throwExceptionWhenServiceDateIsNull`, `throwExceptionWhenStartTimeMissing`, `throwExceptionWhenEndTimeMissing`, `throwExceptionWhenStartTimeNotBeforeEndTime`, `isExpiredAfterDeadline`, `isExpiredBeforeDeadline`, `expireIfDueExpiresActiveReservation`, `expireIfDueKeepsActiveReservation`, `releaseActiveReservation`, `releasePaymentProtectedReservation`, `releaseCommittedReservationThrows`, `releaseTwiceThrows`, `protectPaymentTransitionsToProtected`, `protectPaymentExpiredThrows`, `commitTransitionsToCommitted`, `commitActiveThrows`, `reconstituteReservation`, `isExpiredPaymentProtectedBeforeDeadline`, `isExpiredPaymentProtectedAfterDeadline`, `isExpiredPaymentProtectedWithNullDeadline`

### backend/src/test/java/com/jaldishop/backend/capacity/infrastructure/persistence/mapper/CapacityConfigurationPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `toEntityShouldMapAllFields`, `toDomainShouldMapAllFields`, `roundtripShouldPreserveAllFields`, `fullDayShouldHaveNullTimeFields`

### backend/src/test/java/com/jaldishop/backend/capacity/infrastructure/persistence/mapper/CapacityExceptionPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `toEntityShouldMapAllFields`, `toDomainShouldMapAllFields`, `roundtripShouldPreserveAllFields`, `fullDayShouldHaveNullTimeFields`

### backend/src/test/java/com/jaldishop/backend/capacity/infrastructure/persistence/mapper/CapacityReservationPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `toEntityShouldMapAllFields`, `toDomainShouldMapAllFields`, `roundtripShouldPreserveAllFields`

### backend/src/test/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createSuccess`, `listSuccess`, `updateSuccess`, `activateSuccess`, `deactivateSuccess`, `createForbiddenWhenNotMerchant`

### backend/src/test/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createSuccess`, `listSuccess`, `updateSuccess`, `activateSuccess`, `deactivateSuccess`, `createForbiddenWhenNotMerchant`

### backend/src/test/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `availability`, `createReservation`, `createReservationInvalidBody`, `createReservationExhausted`, `getReservation`, `getForeignReservation`, `releaseReservation`, `forbiddenWhenNotCustomer`, `forbiddenWhenNotCustomerOnCreate`

### backend/src/test/java/com/jaldishop/backend/capacity/web/controller/CapacitySlotsControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `listSlots`, `listSlotsEmpty`, `forbiddenWhenNotCustomer`, `forbiddenWhenNoPrincipal`, `missingStoreId`, `missingDate`, `malformedStoreId`, `malformedDate`, `pastDateRejected`

### backend/src/test/java/com/jaldishop/backend/capacity/web/controller/EffectiveCapacityControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `effectiveCapacityFromBase`, `effectiveCapacityFromException`, `effectiveCapacityNone`, `forbiddenWhenNotMerchant`, `invalidTimeRange`, `missingFranja`

## cart

### backend/src/test/java/com/jaldishop/backend/cart/application/result/CartViewAssemblerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldAssembleEmpty`, `shouldAssemblePopulatedCart`, `shouldHandleMissingVariantFallback`

### backend/src/test/java/com/jaldishop/backend/cart/application/service/AddItemToCartServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldAddItemToCartSuccessfully`, `shouldRejectAddingInactiveVariant`, `shouldRejectAddingItemFromDifferentStore`, `shouldRejectZeroQuantity`

### backend/src/test/java/com/jaldishop/backend/cart/application/service/ClearCartServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldClearCart`, `shouldReturnEmptyWhenCartDoesNotExist`, `shouldThrowWhenStoreNotFound`

### backend/src/test/java/com/jaldishop/backend/cart/application/service/GetCartServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnEmptyCartViewWhenCartDoesNotExist`, `shouldReturnAssembledCartViewWhenCartExists`, `shouldThrowWhenStoreNotFound`

### backend/src/test/java/com/jaldishop/backend/cart/application/service/RemoveCartItemServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldRemoveCartItem`, `shouldThrowWhenCartNotFound`

### backend/src/test/java/com/jaldishop/backend/cart/application/service/UpdateCartItemQuantityServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateCartItemQuantity`, `shouldThrowWhenCartNotFound`

### backend/src/test/java/com/jaldishop/backend/cart/domain/model/CartItemTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateCartItemSuccessfully`, `shouldRejectInvalidQuantity`, `shouldRejectNegativePrice`, `shouldUpdateQuantity`

### backend/src/test/java/com/jaldishop/backend/cart/domain/model/CartTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateCart`, `shouldAddItemsAndAggregateQuantity`, `shouldUpdateQuantityAndRemoveWhenZero`, `shouldRemoveAndClear`

### backend/src/test/java/com/jaldishop/backend/cart/infrastructure/persistence/adapter/CartRepositoryAdapterTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldSaveCart`, `shouldFindByUserIdAndStoreId`, `shouldDeleteById`

### backend/src/test/java/com/jaldishop/backend/cart/infrastructure/persistence/mapper/CartPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `shouldMapDomainToEntityAndBack`, `shouldMapItemEntityToDomain`

### backend/src/test/java/com/jaldishop/backend/cart/web/controller/CartControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldGetCart`, `shouldAddItemToCart`, `shouldUpdateCartItemQuantity`, `shouldRemoveCartItem`, `shouldClearCart`, `shouldRejectAddItemWithZeroQuantity`, `shouldRejectAddItemWithMissingStoreId`, `shouldRejectUpdateItemWithNegativeQuantity`, `shouldReturn409WhenVariantIsInactive`, `shouldReturn409WhenProductStoreMismatch`, `shouldReturn409WhenProductIsInactive`, `shouldReturn404WhenVariantNotFound`, `shouldReturn404WhenStoreNotFoundOnGetCart`

### backend/src/test/java/com/jaldishop/backend/cart/web/mapper/CartResponseMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapCartViewToResponse`, `shouldReturnNullWhenNull`

## catalog

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/CreateCategoryServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateCategorySuccessfully`, `shouldThrowExceptionWhenCategoryNameExistsInStore`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/CreateProductServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateProductWithAutoSlug`, `shouldThrowWhenCategoryDoesNotBelongToStore`, `shouldThrowConflictExceptionWhenSlugExists`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/CreateProductVariantServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateVariantSuccessfully`, `shouldThrowWhenProductNotFound`, `shouldThrowWhenSkuExists`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/GetCategoriesServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListCategoriesByStore`, `shouldGetCategoryByIdAndStore`, `shouldThrowWhenNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/GetProductVariantsServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListVariantsByProduct`, `shouldGetVariantById`, `shouldThrowWhenProductNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/GetProductsServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldGetProductsByStore`, `shouldGetProductsByStoreAndCategory`, `shouldGetProductByIdAndStore`, `shouldThrowWhenNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/GetPublicCategoriesServiceTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`returnsOnlyActiveCategoriesBelongingToTheRequestedStore`, `rejectsInactiveOrMissingStoresBeforeReadingTheirCatalog`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/UpdateCategoryServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateCategorySuccessfully`, `shouldThrowWhenCategoryNotFound`, `shouldThrowWhenDuplicateNameExists`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/UpdateProductServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateProductSuccessfully`, `shouldThrowWhenProductNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/application/service/UpdateProductVariantServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateVariantSuccessfully`, `shouldThrowWhenVariantNotFound`, `shouldThrowWhenSkuExists`

### backend/src/test/java/com/jaldishop/backend/catalog/domain/model/CategoryDomainTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateActiveCategory`, `shouldUpdateCategoryAndStatus`

### backend/src/test/java/com/jaldishop/backend/catalog/domain/model/ProductDomainTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateActiveProduct`, `shouldUpdateProductAndState`

### backend/src/test/java/com/jaldishop/backend/catalog/domain/model/ProductImageTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createProductImageSuccessfully`, `throwWhenInvalidFields`

### backend/src/test/java/com/jaldishop/backend/catalog/domain/model/ProductVariantDomainTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateVariantSuccessfully`, `shouldRejectInvalidPrice`

### backend/src/test/java/com/jaldishop/backend/catalog/infrastructure/persistence/mapper/CategoryPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapDomainToEntity`, `shouldMapEntityToDomain`

### backend/src/test/java/com/jaldishop/backend/catalog/infrastructure/persistence/mapper/ProductPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapBetweenDomainAndEntity`

### backend/src/test/java/com/jaldishop/backend/catalog/infrastructure/persistence/mapper/ProductVariantPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapVariantAndAttributes`

### backend/src/test/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateCategory`, `shouldGetCategories`, `shouldReturn403WhenStoreNotOwnedOnCreate`, `shouldReturn403WhenStoreNotOwnedOnGet`, `shouldReturn409WhenCategoryAlreadyExists`, `shouldReturn404WhenCategoryNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/web/controller/MerchantProductControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateProduct`, `shouldGetProductById`, `shouldReturn403WhenStoreNotOwnedOnCreate`, `shouldReturn403WhenStoreNotOwnedOnGet`, `shouldReturn409WhenProductSlugAlreadyExists`, `shouldReturn404WhenCategoryNotFoundOnCreate`, `shouldReturn404WhenProductNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateVariant`, `shouldRejectInvalidCurrency`, `shouldGetVariants`, `shouldReturn403WhenStoreNotOwnedOnCreate`, `shouldReturn409WhenSkuAlreadyExists`, `shouldReturn404WhenVariantNotFound`

### backend/src/test/java/com/jaldishop/backend/catalog/web/controller/PublicCategoryControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnCategoriesByStoreId`, `shouldRejectTooLongSlug`

### backend/src/test/java/com/jaldishop/backend/catalog/web/controller/PublicProductControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldGetActiveProductsByStoreId`, `shouldGetActiveProductsBySlug`, `shouldRejectTooLongSlug`

### backend/src/test/java/com/jaldishop/backend/catalog/web/mapper/CategoryResponseMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapCategoryToResponse`, `shouldReturnNullWhenCategoryIsNull`

### backend/src/test/java/com/jaldishop/backend/catalog/web/mapper/ProductResponseMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapProductToResponse`, `shouldMapProductWithVariants`, `shouldReturnNullWhenProductIsNull`

### backend/src/test/java/com/jaldishop/backend/catalog/web/mapper/ProductVariantResponseMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapVariantToResponse`, `shouldReturnNullWhenVariantIsNull`

## checkout

### backend/src/test/java/com/jaldishop/backend/checkout/application/result/CheckoutItemSnapshotAssemblerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldAssembleSnapshotsSuccessfully`, `shouldReturnEmptyListWhenCartNull`, `shouldThrowNotFoundWhenVariantMissing`, `shouldThrowConflictWhenVariantInactive`, `shouldThrowConflictWhenStoreMismatch`, `shouldThrowConflictWhenProductInactive`

### backend/src/test/java/com/jaldishop/backend/checkout/application/service/CheckoutHoldConcurrencyIntegrationTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`initDatabase`, `cleanUpTestData`, `awaitFutures`, `createTestUser`, `createVariant`, `setStock`, `test1_lastStockConcurrentCheckout`, `test2_partialQuantityConcurrentCheckout`, `test3_multipleVariantsDeadlockPrevention`, `test4_rollbackOnInsufficientInventory`, `test5_capacityExhaustedNoInventoryReservation`, `test6_logicalExpirationWithoutWorker`, `test7_untrackedVariantCheckoutAllowedWithoutInventory`, `test8_trackedVariantWithoutInventoryThrowsAndRollbacks`, `test9_repeatedVariantGroupedCorrectly`

### backend/src/test/java/com/jaldishop/backend/checkout/application/service/ConfirmPurchaseConcurrencyIntegrationTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`initDatabase`, `cleanUpTestData`, `awaitFutures`, `createTestUser`, `createVariant`, `setStock`, `insertInventoryHold`, `insertPayment`, `insertOrder`, `test1_concurrentConfirmSameReservationIsIdempotent`, `test2_ownHoldExcludedFromOtherActiveGuard`, `test3_otherActiveHoldPreventsOversell`, `test4_expiredPaymentProtectionNeverCommits`, `test5_orderUniqueBackstop`

### backend/src/test/java/com/jaldishop/backend/checkout/application/service/ConfirmPurchaseServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `stubIdempotencyMiss`, `shouldConfirmPurchaseSuccessfullyForPickup`, `shouldConfirmPurchaseSuccessfullyForDelivery`, `shouldRejectPaymentNotApproved`, `shouldThrowWhenPaymentNotFound`, `shouldReturnExistingOrderForDuplicatePayment`, `shouldReturnRaceWinnerByCommittedReservation`, `shouldPropagateConflictWhenReservationNotProtected`, `shouldThrowConflictWhenCartEmpty`, `shouldThrowConflictWhenStoreInactive`, `shouldConfirmPurchaseSuccessfullyFromImmutableSnapshotWithoutReadingCatalog`, `shouldPreserveCheckoutPricesEvenIfCatalogPricesChangedLater`, `shouldSelectivelyRemoveOnlyCommittedItemsAndKeepNewItemsInCart`, `shouldSelectivelyDecrementQuantityWhenCartHasHigherQuantity`, `shouldPreserveCartLineWhenCartHasFewerUnitsThanPurchased`, `shouldNotRecreateCartItemIfUserDeletedLineAfterCheckout`, `shouldFailAndRollbackWhenInventoryReservationFails`, `shouldFailAndRollbackWhenCapacityReservationExpires`, `shouldRejectWhenItemsSnapshotIsCorruptOrInconsistent`, `assertRuleCode`

### backend/src/test/java/com/jaldishop/backend/checkout/application/service/InitiateCheckoutServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldInitiateCheckoutSuccessfullyForPickup`, `shouldInitiateCheckoutSuccessfullyForDeliveryWithDeliveryFee`, `shouldThrowConflictWhenCartIsEmpty`, `shouldThrowConflictWhenStoreIsInactive`, `shouldThrowConflictWhenDeliveryNotAvailable`, `shouldThrowBusinessRuleWhenDeliveryAddressMissing`, `shouldThrowConflictWhenPickupNotAvailable`, `shouldPropagateConflictWhenCapacityExhausted`

### backend/src/test/java/com/jaldishop/backend/checkout/domain/model/CheckoutPricingTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCalculatePricingForPickup`, `shouldCalculatePricingForDelivery`, `shouldHandleEmptyListAndDefaults`

### backend/src/test/java/com/jaldishop/backend/checkout/domain/service/CheckoutFulfillmentValidatorTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldValidateDeliverySuccessfully`, `shouldThrowConflictWhenDeliveryDisabled`, `shouldThrowBusinessRuleWhenDeliveryAddressMissing`, `shouldValidatePickupSuccessfully`, `shouldThrowConflictWhenPickupDisabled`

### backend/src/test/java/com/jaldishop/backend/checkout/web/controller/CheckoutControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldInitiateCheckoutSuccessfully`, `shouldReturn400WhenValidationFails`, `shouldReturn409WhenCartIsEmpty`, `shouldReturn409WhenCapacityExhausted`

## database

### backend/src/test/java/com/jaldishop/backend/database/PostgreSqlArchitectureIntegrationTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`initDatabase`, `execute`, `assertThrowsInSavepoint`, `testCustomerMultiStoreAndUniquePerStore`, `testRoleScopeCheckConstraint`, `testStoreOwnerUniqueness`, `testProductImagesPrimaryConstraint`, `testInventoryReservationTenantSafe`, `testCapacitySlotOverlapsExcludeConstraint`, `testQuantityChecks`

## e2e

### backend/src/test/java/com/jaldishop/backend/e2e/EndToEndBackendFlowValidationIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`startMockServerAndSeed`, `handle`, `handle`, `applySeedScript`, `stopMockServer`, `configureDynamicProperties`, `test1_verifySeedDataIntegrity`, `test2_authenticateUsers`, `test3_browseStoreAndCatalog`, `test4_checkCustomerAddressBook`, `test5_initiateCheckoutHold`, `test6_simulatePaymentAndOrderConfirmation`, `test7_merchantOrderStatusTransitions`, `test8_negativeInsufficientStock`, `test9_negativeCapacityExhausted`, `test10_negativeDuplicateWebhookIdempotency`, `test11_negativeRejectedPaymentDoesNotConfirmOrder`

## favorite

### backend/src/test/java/com/jaldishop/backend/favorite/application/service/AddFavoriteProductServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldAddFavoriteWhenNotAlreadyMarked`, `shouldBeIdempotentWhenAlreadyFavorite`, `shouldThrowWhenProductDoesNotExist`

### backend/src/test/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnFavoriteProducts`, `shouldAddFavoriteProduct`, `shouldRemoveFavoriteProduct`, `shouldCheckFavoriteStatus`

## identity

### backend/src/test/java/com/jaldishop/backend/identity/BCryptHashVerificationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`testPasswordMatches`

### backend/src/test/java/com/jaldishop/backend/identity/application/service/ChangeUserStatusServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldSuspendActiveUser`, `shouldThrowWhenAdminSuspendsSelf`, `shouldThrowWhenUserAlreadySuspended`, `shouldThrowConflictWhenUserAlreadyActive`, `shouldActivateSuspendedUser`, `shouldThrowWhenUserNotFound`

### backend/src/test/java/com/jaldishop/backend/identity/application/service/GetAdminUsersServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListUsers`, `shouldGetUserById`, `shouldThrowWhenUserNotFound`

### backend/src/test/java/com/jaldishop/backend/identity/application/service/RefreshTokenServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `executeSuccess`, `executeUserNotFound`

### backend/src/test/java/com/jaldishop/backend/identity/application/service/RegisterMerchantRollbackIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureTestDatabase`, `cleanPreexistingData`, `cleanTestDataAfterRun`, `cleanTestData`, `registerMerchantCreatesUserRolesAndStore`, `storeCreationFailureRollsBackInsertedUser`

### backend/src/test/java/com/jaldishop/backend/identity/application/service/RegisterMerchantServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `registerNewMerchantSuccessfully`, `upgradeExistingCustomerToMerchantSuccessfully`, `throwExceptionWhenExistingUserIsAlreadyMerchant`, `throwExceptionWhenExistingCustomerPasswordDoesNotMatch`, `throwExceptionWhenCreateStoreFails`, `provisionStoreWithExactlyTwentyMappedFields`

### backend/src/test/java/com/jaldishop/backend/identity/application/service/UpdateUserRolesServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateUserRolesToMerchant`, `shouldThrowWhenAdminRemovesOwnAdminRole`, `shouldThrowWhenRolesEmpty`, `shouldThrowWhenUserNotFound`

### backend/src/test/java/com/jaldishop/backend/identity/domain/model/RoleTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createRoleWithValidValues`, `createRoleWithAllEnumValues`, `throwExceptionWhenIdIsNull`, `throwExceptionWhenNameIsNull`

### backend/src/test/java/com/jaldishop/backend/identity/domain/model/UserTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createUserWithValidValues`, `createUserWithMultipleRoles`, `throwExceptionWhenEmailIsNull`, `throwExceptionWhenEmailIsBlank`, `throwExceptionWhenPasswordIsNull`, `throwExceptionWhenPasswordIsBlank`, `throwExceptionWhenFirstNameIsNull`, `throwExceptionWhenFirstNameIsBlank`, `throwExceptionWhenLastNameIsNull`, `throwExceptionWhenLastNameIsBlank`, `throwExceptionWhenRolesIsNull`, `throwExceptionWhenRolesIsEmpty`, `emailShouldBeConvertedToLowercase`, `emailShouldBeTrimmed`, `emailShouldBeNormalizedCorrectly`, `canAuthenticateShouldReturnTrueWhenActive`, `canAuthenticateShouldReturnFalseWhenInactive`, `canAuthenticateShouldReturnFalseWhenSuspended`, `getFullNameShouldReturnCorrectFormat`, `getFullNameShouldWorkWithCompoundNames`, `validateRequiredShouldThrowExceptionWithNull`, `validateRequiredShouldThrowExceptionWithEmpty`, `validateRequiredShouldThrowExceptionWithBlank`, `validateRequiredShouldNotThrowExceptionWithValidValue`, `addRoleShouldAddRoleAndSetUpdatedAt`, `addRoleWithNullShouldThrowException`, `hasRoleShouldReturnFalseWhenNotAssignedOrNull`, `getRolesShouldReturnUnmodifiableSet`, `updateRolesShouldReplaceRoles`, `updateRolesShouldThrowWhenNullOrEmpty`, `suspendShouldChangeStatusAndThrowConflictWhenAlreadySuspended`, `suspendShouldRejectSelfSuspension`, `activateShouldChangeStatusAndThrowConflictWhenAlreadyActive`

### backend/src/test/java/com/jaldishop/backend/identity/infrastructure/persistence/mapper/UserPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `toEntityShouldPreserveUuid`, `toEntityShouldPreserveEmail`, `toEntityShouldPreservePassword`, `toEntityShouldPreserveStatus`, `toEntityShouldMapRoles`, `toEntityShouldPreserveNames`, `toEntityShouldPreservePhone`, `toEntityShouldPreserveTimestamps`, `toDomainShouldReconstructUuid`, `toDomainShouldPreserveEmail`, `toDomainShouldPreservePassword`, `toDomainShouldPreserveStatus`, `toDomainShouldReconstructRoles`, `toDomainShouldPreserveTimestamps`, `toDomainShouldPreserveNames`, `toDomainShouldPreservePhone`, `roundtripShouldPreserveAllFields`, `roundtripShouldPreserveInactiveStatus`, `roundtripShouldPreserveSuspendedStatus`, `roundtripShouldPreserveNullPhone`

### backend/src/test/java/com/jaldishop/backend/identity/infrastructure/security/JwtAuthenticationFilterTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `tearDown`, `whenUserIsActive_withValidJwt_shouldAuthenticateAndSetAuthorities`, `whenUserIsSuspended_withValidJwt_shouldNotAuthenticateAndClearContext`, `whenUserIsInactive_withValidJwt_shouldNotAuthenticateAndClearContext`, `whenUserDoesNotExist_withValidJwt_shouldNotAuthenticateAndClearContext`, `whenJwtIsInvalidOrExpired_shouldClearContextAndContinueChain`, `whenJwtMalformed_shouldClearContextAndContinueChain`, `whenDatabaseFailsWithDataAccessException_shouldReturn503AndHaltChain`, `whenTransactionFails_shouldReturn503AndHaltChain`, `whenPersistenceException_shouldReturn503AndHaltChain`, `whenProgrammingErrorOccurs_shouldNotCatchIndiscriminately`, `whenUserIsReactivated_shouldAuthenticateSuccessfully`, `whenNoAuthorizationHeader_shouldPassThroughWithoutModifyingContext`, `whenAuthorizationHeaderNotBearer_shouldPassThroughWithoutModifyingContext`, `roleBasedAuthorities_shouldMapAllRolesWithPrefix`

### backend/src/test/java/com/jaldishop/backend/identity/infrastructure/security/SecurityUserStatusWebTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `whenUserIsActive_withValidJwt_shouldAllowAccess`, `whenUserIsSuspended_withValidJwt_shouldReturn401`, `whenUserIsInactive_withValidJwt_shouldReturn401`, `whenUserDeletedOrNotFound_withValidJwt_shouldReturn401`, `whenJwtIsInvalidOrExpired_shouldReturn401`, `whenUserSuspended_onPublicRoute_shouldAllowAnonymousAccess`, `whenJwtMalformed_onPublicRoute_shouldAllowAnonymousAccess`, `whenUserReactivated_previousJwtBecomesValidAgain`, `whenUserRepositoryFails_onProtectedRoute_shouldReturn503ServiceUnavailable`, `whenUserRepositoryFails_onPublicRouteWithJwt_shouldReturn503ServiceUnavailable`, `whenAnonymousRequestToPublicRoute_shouldSucceed`, `whenAdminUser_accessAdminRoute_shouldSucceed`, `whenCustomerUser_accessAdminRoute_shouldReturn403Forbidden`

### backend/src/test/java/com/jaldishop/backend/identity/web/controller/AdminUserControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListUsers`, `shouldGetUserById`, `shouldSuspendUser`, `shouldActivateUser`, `shouldUpdateUserRoles`, `listUsersInvalidQueryParameter`, `shouldReturn409WhenUserAlreadySuspended`, `shouldReturn409WhenUserAlreadyActive`

### backend/src/test/java/com/jaldishop/backend/identity/web/controller/AuthControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `registerMerchantSuccessfully`, `registerMerchantValidationErrors`, `refreshTokenSuccessfully`

### backend/src/test/java/com/jaldishop/backend/identity/web/controller/UserControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `getMyProfileSuccess`, `getMyProfileUnauthorized`, `getMyProfileNotFound`, `updateProfileSuccess`, `updateProfileValidationMissingFirstName`, `updateProfileValidationMissingLastName`, `updateProfileValidationInvalidPhone`, `updateProfileMalformedJson`, `updateProfileUnauthorized`

### backend/src/test/java/com/jaldishop/backend/identity/web/mapper/AdminUserResponseMapperTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `toSummaryShouldMapCorrectly`, `toDetailShouldMapCorrectly`

## inventory

### backend/src/test/java/com/jaldishop/backend/inventory/application/service/InventoryConcurrencyUnitTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `testConcurrentStockInitialization`, `testConcurrentStockAdjustmentFailsIfBelowActiveHold`

### backend/src/test/java/com/jaldishop/backend/inventory/application/service/InventoryRealConcurrencyPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureProperties`, `migrateDb`, `testRealConcurrentHoldAndManualAdjustment`, `testRealMultithreadedPutStockPostgres`, `testConcurrentPutStockAndReserveInventoryPostgres`, `testConcurrentPutStockAndCommitInventoryPostgres`, `testConcurrentPutStockAndRestockInventoryPostgres`

### backend/src/test/java/com/jaldishop/backend/inventory/application/service/InventoryServicesTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `testGetStoreInventory`, `testGetVariantNotTracked`, `testGetVariantNotConfigured`, `testUpdateInventoryStockSuccess`, `testUpdateInventoryStockTracksFalseThrows`, `testUpdateInventoryStockBelowReservedThrows`, `testUpdateThresholdSuccess`, `testUpdateThresholdFailsWhenInventoryNotConfigured`, `testUpdateThresholdNullToDisableAlerts`

### backend/src/test/java/com/jaldishop/backend/inventory/application/service/ReserveInventoryServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnEmptyWhenNoItemsTrackInventory`, `shouldThrowInventoryNotConfiguredWhenRowMissing`, `shouldThrowInsufficientStockWhenAvailableLessThanRequested`, `shouldGroupRepeatedVariantsAndLockCanonically`

### backend/src/test/java/com/jaldishop/backend/inventory/application/service/RestockInventoryServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldRestockInventoryAndMarkReservationsReleased`, `shouldBeIdempotentWhenNoCommittedReservationsExist`, `shouldDoNothingWhenCapacityReservationIdIsNull`

### backend/src/test/java/com/jaldishop/backend/inventory/domain/model/InventoryModelTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`validInventoryCreation`, `isLowStockWhenQuantityLessOrEqualThreshold`, `isLowStockFalseWhenThresholdNull`, `invalidArgumentsThrowException`

### backend/src/test/java/com/jaldishop/backend/inventory/domain/model/InventoryReservationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createInventoryReservation`, `lifecycleTransitions`, `throwWhenInvalidQuantityOrStoreId`

### backend/src/test/java/com/jaldishop/backend/inventory/infrastructure/persistence/InventoryJpaRepositorySpringDataTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureProperties`, `migrateDb`, `testFindAllVariantInventoriesByStoreId`, `testFindVariantInventoryByStoreIdAndVariantId`, `testUpsertStockAndRefreshPersistenceContext`

### backend/src/test/java/com/jaldishop/backend/inventory/infrastructure/persistence/InventoryPostgresIntegrationTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`initDatabase`, `testFindAllVariantInventoriesByStoreId`, `testLowStockQuery`, `testNativeUpsert`

### backend/src/test/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `testGetStoreInventory`, `testGetVariantInventory`, `testGetLowStock`, `testUpdateStockSuccess`, `testUpdateStockNegativeThrows`, `testUpdateThresholdSuccess`, `testUpdateThresholdNotConfiguredThrowsConflict`

## media

### backend/src/test/java/com/jaldishop/backend/media/application/service/GenerateUploadSignatureServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `generateSignatureForMerchant`, `generateSignatureWithExplicitStoreId`, `throwExceptionWhenStoreNotFound`, `allowBrandingSignatureWhenStoreNotFound`

### backend/src/test/java/com/jaldishop/backend/media/application/service/StoreMediaCleanupAdapterTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`shouldDelegateToMediaStorageService`, `shouldPropagateFailureToCaller`

### backend/src/test/java/com/jaldishop/backend/media/infrastructure/cloudinary/CloudinaryMediaStorageServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `generateSignatureSuccessfully`, `deleteStoreMediaWithNullStoreId`, `deleteStoreMediaHandlesCloudinaryExceptionGracefully`

### backend/src/test/java/com/jaldishop/backend/media/web/controller/MediaControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `generateUploadSignatureSuccess`, `rejectUnauthorizedUserForProduct`, `allowStoreLogoForCustomerDuringOnboarding`

## notification

### backend/src/test/java/com/jaldishop/backend/notification/application/service/CountUnreadNotificationsServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `countUnreadNotifications`, `returnZeroWhenNoUnread`

### backend/src/test/java/com/jaldishop/backend/notification/application/service/CreateNotificationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createNotificationSuccessfully`, `createSystemNotification`, `createLowStockNotification`

### backend/src/test/java/com/jaldishop/backend/notification/application/service/ListUserNotificationsServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `listUserNotifications`, `returnEmptyListWhenNoNotifications`

### backend/src/test/java/com/jaldishop/backend/notification/application/service/MarkNotificationAsReadServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `markAsReadSuccessfully`, `throwExceptionWhenNotificationNotFound`, `throwExceptionWhenNotificationDoesNotBelongToUser`

### backend/src/test/java/com/jaldishop/backend/notification/domain/model/NotificationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createNotificationWithValidValues`, `createNotificationWithAllTypes`, `throwExceptionWhenUserIdIsNull`, `throwExceptionWhenTypeIsNull`, `throwExceptionWhenTitleIsNull`, `throwExceptionWhenTitleIsBlank`, `throwExceptionWhenMessageIsNull`, `throwExceptionWhenMessageIsBlank`, `markAsReadShouldChangeStatusAndSetReadAt`, `markAsReadShouldThrowExceptionIfAlreadyRead`, `reconstituteNotification`, `titleAndMessageShouldBeTrimmed`

### backend/src/test/java/com/jaldishop/backend/notification/infrastructure/listener/OrderNotificationEventListenerTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `shouldPersistNotificationSuccessfully`, `shouldSkipPersistenceWhenStoreOwnerNotFound`, `shouldHandleDeduplicationKeyCollisionGracefully`, `shouldCatchUnexpectedDatabaseExceptionsWithoutThrowing`

### backend/src/test/java/com/jaldishop/backend/notification/infrastructure/persistence/NotificationDeduplicationExceptionClassifierTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldClassifyTrueWhenConstraintAndSqlStateMatch`, `shouldClassifyFalseWhenConstraintIsDifferent`, `shouldClassifyFalseWhenForeignKeyViolation`, `shouldClassifyFalseForUnrelatedExceptions`

### backend/src/test/java/com/jaldishop/backend/notification/infrastructure/persistence/OrderNotificationPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`initSchemaAndData`, `cleanUpTestData`, `cleanTestData`, `setUpMockMvc`, `insertTestUser`, `insertTestStore`, `testFlywayV16MigrationAppliesCleanly`, `testHistoricalRowsCompatibility`, `testSingleOrderGeneratesPersistentNotification`, `testSequentialDuplicateIsIdempotent`, `testConcurrentDuplicateInserts`, `testNonUniqueIntegrityViolationIsLoggedAsError`, `testCheckoutRollbackDoesNotTriggerNotification`, `testMerchantIsolationAcrossStores`, `testN1EndpointsReadNewNotification`, `testCheckoutHttpResponsePreservedWhenNotificationListenerFailsPostCommit`

### backend/src/test/java/com/jaldishop/backend/notification/infrastructure/persistence/mapper/NotificationPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `toEntityShouldMapAllFields`, `toDomainShouldMapAllFields`, `shouldHandleNullReadAt`, `roundtripShouldPreserveAllFields`

### backend/src/test/java/com/jaldishop/backend/notification/web/controller/NotificationControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `listNotificationsSuccess`, `listNotificationsEmpty`, `countUnreadSuccess`, `markAsReadSuccess`, `markAsReadNotFound`, `markAsReadAlreadyRead`, `listNotificationsInvalidParameters`

## ordering

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/CancelCustomerOrderServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCancelOrderSuccessfullyWhenCustomerIsOwnerAndStatusIsConfirmed`, `shouldRejectCancellationWhenOrderBelongsToAnotherCustomer`, `shouldRejectCancellationWhenOrderIsInPreparation`, `shouldBeIdempotentWhenOrderIsAlreadyCancelled`, `shouldThrowWhenOrderDoesNotExist`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/ChangeOrderStatusConcurrencyIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureTestDatabase`, `resetMocks`, `cleanUpTestData`, `shouldSerializeConcurrentStatusChangesWithoutLostUpdate`, `shouldBeIdempotentAndSerializeWhenTwoThreadsAttemptSameTransition`, `shouldNotPublishWebSocketEventWhenTransactionRollsBack`, `shouldPublishWebSocketEventWhenTransactionCommits`, `seedOrderInPostgres`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/ChangeOrderStatusServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldChangeStatusSuccessfullyWhenStoreIsAuthorized`, `shouldRejectWhenOrderBelongsToAnotherStore`, `shouldRejectWhenExpectedStoreIdIsNull`, `shouldBeNoOpWhenStatusIsAlreadySame`, `shouldThrowRNORD07WhenTransitionIsInvalid`, `shouldThrowExceptionWhenOrderNotFound`, `shouldPublishEventsForSubsequentValidTransitions`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/GetCustomerOrderDetailServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnOrderDetailWhenBelongsToCustomer`, `shouldThrowNotFoundWhenOrderBelongsToAnotherCustomer`, `shouldThrowNotFoundWhenOrderDoesNotExist`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/GetCustomerOrdersServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnCustomerOrders`, `shouldThrowWhenUserIdIsNull`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/GetMerchantOrderDetailServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldThrowExceptionWhenOrderIdIsNull`, `shouldThrowExceptionWhenStoreIdIsNull`, `shouldReturnOrderDetailWhenExistsInStore`, `shouldThrowResourceNotFoundExceptionWhenOrderDoesNotExist`, `shouldThrowResourceNotFoundWhenOrderBelongsToAnotherStore`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/ListMerchantOrdersServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldThrowExceptionWhenStoreIdIsNull`, `shouldListOrdersWithoutFilterWithPagination`, `shouldListOrdersWithStatusFilter`, `shouldDefaultToFirstPageWhenPageableIsNull`, `shouldReturnEmptyPageWhenStoreHasNoOrders`, `shouldHandleOutOfRangePageGracefully`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/OrderCancellationCompensatorTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCompensateCapacityInventoryAndRequestRefundWhenCancelledFromConfirmed`, `shouldRequestRefundOnlyWithoutRestitutingCapacityOrInventoryWhenCancelledFromInPreparation`, `shouldNotDuplicateRefundIfAlreadyPendingOrCompleted`

### backend/src/test/java/com/jaldishop/backend/ordering/application/service/OrderCancellationPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureTestDatabase`, `initSchema`, `resetMocks`, `cleanUpTestData`, `shouldCancelConfirmedOrderAsCustomerAndCompensateResources`, `shouldCancelConfirmedOrderAsMerchantAndCompensateResources`, `shouldNotCompensateCapacityOrInventoryWhenMerchantCancelsFromInPreparation`, `shouldRejectCancellationOfNonOwnedOrder`, `shouldRejectCancellationFromInvalidState`, `shouldBeIdempotentOnRepeatedCustomerCancellation`, `shouldSerializeConcurrentCancellationsWithoutDoubleRestock`, `shouldRollbackCompleteWhenCompensationFails`, `shouldSerializeConcurrentlyWithInventoryOperationWithoutDeadlock`, `shouldAllowRefundProcessorToClaimRefundForCancelledOrderAndBlockActiveOrder`, `seedUser`, `seedPaymentAttempt`

### backend/src/test/java/com/jaldishop/backend/ordering/domain/model/OrderTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateConfirmedOrderWithPickup`, `shouldRejectBlankOrderNumber`, `shouldRejectNullUser`, `shouldRejectNullStore`, `shouldRejectNullPayment`, `shouldRejectNullReservation`, `shouldRejectNullDeliveryMode`, `shouldRejectDeliveryWithoutAddress`, `shouldRejectEmptyItems`, `shouldRejectBlankCurrency`, `shouldRejectNegativeAmounts`, `shouldRejectDiscountAboveSubtotal`, `shouldTransitionAndAppendHistory`, `shouldRejectInvalidTransition`, `shouldRejectOutForDeliveryForPickup`, `assertRuleCode`

### backend/src/test/java/com/jaldishop/backend/ordering/infrastructure/persistence/OrderPersistenceIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureTestDatabase`, `seedDatabase`, `cleanUpTestData`, `shouldPaginateOrdersFirstPageDescending`, `shouldPaginateOrdersSecondPage`, `shouldFilterOrdersByStatusWithPagination`, `shouldIsolateOrdersBetweenStoresInPagination`, `shouldFindByIdAndStoreIdWithStrictOwnership`, `shouldFindByOrderNumberAndStoreIdWithStrictOwnership`, `seedOrder`

### backend/src/test/java/com/jaldishop/backend/ordering/infrastructure/persistence/adapter/JpaOrderRepositoryAdapterTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldFindByStoreIdWithPagination`, `shouldFindByStoreIdAndStatusWithPagination`, `shouldFindByIdAndStoreIdWhenExists`, `shouldReturnEmptyWhenOrderNotFoundForStore`, `shouldFindByOrderNumberAndStoreIdWhenExists`, `shouldReturnEmptyWhenOrderNumberNotFoundForStore`, `shouldSaveOrder`, `shouldFindByIdForUpdate`

### backend/src/test/java/com/jaldishop/backend/ordering/infrastructure/persistence/mapper/OrderPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `shouldMapDomainToEntity`, `shouldMapEntityToDomain`, `shouldPreserveStatusHistoryAfterTransitionRoundTrip`, `shouldReturnNullForNullInput`

### backend/src/test/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnCustomerOrders`, `shouldReturnOrderDetail`, `shouldReturn404WhenNotFoundOrBelongsToAnotherCustomer`, `shouldCancelOrderSuccessfully`, `shouldReturn422WhenOrderIsNotInConfirmedStatus`

### backend/src/test/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListOrdersForAuthorizedMerchant`, `shouldListOrdersWithStatusFilterAndCustomPagination`, `shouldGetOrderDetailWhenAuthorized`, `shouldReturn404WhenOrderNotFound`, `shouldReturn403WhenMerchantAttemptsAccessToAnotherStore`, `shouldReturn403WhenCustomerAttemptsAccessToMerchantEndpoints`, `shouldReturn401WhenPrincipalIsNull`, `shouldChangeOrderStatusSuccessfully`, `shouldHandleIdempotentStatusChange`, `shouldReturn422WhenTransitionIsInvalid`, `shouldReturn400WhenParametersOrBodyAreInvalid`, `shouldVerifyAuthorizationPrecedesBusinessServices`

### backend/src/test/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderEndToEndIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureTestDatabase`, `seedDatabase`, `setupTokens`, `tearDownSessions`, `cleanUpTestData`, `shouldListOrdersForAuthorizedMerchantOverHttp`, `shouldFilterOrdersByStatusOverHttp`, `shouldGetOrderDetailOverHttp`, `shouldRejectCrossStoreAccess`, `shouldRejectCustomerAccessToMerchantEndpoints`, `shouldRejectUnauthenticatedRequest`, `shouldReturn404ForNonExistentOrCrossStoreOrder`, `shouldChangeOrderStatusAndEmitWebSocketEventToOwnerCustomer`, `shouldHandleIdempotentPatchWithoutDuplicateStompNotification`, `shouldRejectInvalidTransitionWithoutEmittingStomp`, `shouldReturn400ForInvalidParameters`, `shouldExecuteExactly4SqlQueriesFor20OrdersPage`, `seedOrderInPostgres`, `handleFrame`

## payment

### backend/src/test/java/com/jaldishop/backend/payment/application/MercadoPagoOrdersApiEndToEndIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`startMockServer`, `handle`, `stopMockServer`, `configureIsolatedProperties`, `setupAndClean`, `createCartWithItem`, `creationIsCommittedAndRowsAreUnlockedDuringPost`, `twoAttemptsOfSamePaymentEmitDistinctDurableReferences`, `concurrentDifferentBodiesForSameAttemptHaveOnlyOneWinner`, `concurrentCheckoutRequestsSendOnlyOnePost`, `failureSavingRemoteSuccessDoesNotAuthorizeAnotherPost`, `deferredCommitFailurePreventsPostAndRollsBackRequest`, `testE2E01_successfulOrderCreationWithOrdersApi`, `testE2E02_legacyPreferencesFlowRegression`, `testE2E03_orderCreatedWithoutFinancialTransactions`, `testE2E04_orderWithPendingPayment_transitionsToProcessing`, `testE2E05_accreditedPayment_confirmsCommercialOrder`, `testE2E06_duplicateWebhook_appliesIdempotentlyWithoutDegradation`, `testE2E07_concurrentWebhooks_serializesViaPessimisticLock`, `testE2E08_invalidHmacSignature_rejectedWithSecurityException`, `testE2E09_webhookWithMismatchedOrNonExistentExternalReference`, `testE2E10_paymentAccreditedAfterCapacityReservationExpired_recordsIncidence`, `testE2E11_failureBetweenFinancialAccreditationAndCommercialConfirmation`, `testE2E12_recoveryViaReconciliationScheduler`, `testE2E13_timeoutDuringOrderCreation_doesNotResend`, `testE2E14_featureFlagSwitchBetweenPreferencesAndOrders`, `testE2E15_duplicateProviderOrderId_enforcedByPostgresUniqueConstraint`

### backend/src/test/java/com/jaldishop/backend/payment/application/PaymentFlowEndToEndIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`startMockServer`, `handle`, `stopMockServer`, `configureIsolatedProperties`, `cleanData`, `scenarioA_approvedPaymentAndOrderConfirmedWithIdempotentWebhook`, `scenarioB_approvedPaymentWithCommercialFailureAndFullRefund`, `scenarioC_transientTechnicalFailureAndReconciliationRecovery`, `scenarioD_financialTimeoutAndReconciliationViaGet`, `scenarioE_concurrencyWebhooksReconcilerAndRefundWorkers`, `scenarioF_incidencePersistenceErrorAndTransactionalRecovery`, `scenarioG1_webhookWithInvalidSignatureThrowsSecurityException`, `scenarioG2_mismatchedCollectorIdThrowsSecurityException`, `scenarioG3_inconsistentEventIdsThrowIllegalArgumentException`, `scenarioG4_multipleApprovedAttemptsEscalateToManualReview`

### backend/src/test/java/com/jaldishop/backend/payment/application/PaymentSchedulerConditionsIntegrationTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`whenPropertiesDisabled_noSchedulersInstantiated`, `whenOnlyReconciliationEnabled_onlyReconciliationSchedulerInstantiated`, `whenOnlyRefundEnabled_onlyRefundSchedulerInstantiated`, `whenBothEnabled_bothSchedulersInstantiated`

### backend/src/test/java/com/jaldishop/backend/payment/application/gateway/ExternalPaymentReferenceTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`parsesLegacyPaymentAndVersionedAttempt`, `rejectsUnknownInvalidAndNonCanonicalReferences`

### backend/src/test/java/com/jaldishop/backend/payment/application/gateway/MercadoPagoResourceValidatorTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`blankTransactionIdentityIsRejected`, `expectedAccountAndEnvironmentMustBePresentAndMatch`

### backend/src/test/java/com/jaldishop/backend/payment/application/investigation/ManualOrderInvestigationPostgresIntegrationTest.java

Tipo: ParameterizedTest, SpringBootTest; requiere revisar PostgreSQL/contexto real.

`databaseProperties`, `createIsolatedFixture`, `removeOnlyOwnFixture`, `concurrentAttemptReferenceSnapshotsHaveOneWinnerAndStayDurable`, `verifiesHistoricalReferenceAndCommitsAssociationWithBothAudits`, `jpaFlushFailureRollsBackPreviouslyInsertedJdbcAudit`, `deferredJdbcAuditFailureRollsBackFlushedJpaAssociation`, `twoInvestigationsSerializeAndCreateOnlyOneAssociation`, `webhookHoldingPaymentCanUpdateAttemptWhileInvestigationWaits`, `webhookApprovalAndCompletedRefundDuringGetArePreserved`, `duplicateAndLateWebhookPreserveFinancialEvidence`, `contradictoryWebhookCommitsAuditWithoutFinancialMutation`, `remoteGetSuspendsCallerTransactionAndRestoresItAfterwards`, `noKnownIdRemainsUnknownWithoutAnyHttpRequestOrNewAttempt`, `remoteErrorsPreserveUnknownWithoutRetryingCreation`, `expectValidGet`, `assertOutsideTransaction`, `assertCreated`, `assertUnknownWithNoCheckout`, `waitFor`, `cross`

### backend/src/test/java/com/jaldishop/backend/payment/application/investigation/ManualOrderInvestigationServiceTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setup`, `doBegin`, `doCommit`, `doRollback`, `knownId`, `explicitReferenceRequiresMatchingSnapshotAndAttempt`, `noIdDoesNotSearchOrRetry`, `locksPaymentBeforeAttemptAfterRemoteGet`, `auditOnlyPathAlsoLocksPaymentBeforeAttempt`, `changedOwnerUnderLocksPreventsAssociation`, `auditOnlyPathRevalidatesOwnerUnderLocks`, `historicalReferenceVerifiedRepeatedlyWithoutFinancialWrites`, `mismatchedReferenceContradicts`, `webhookCreationEvidenceWins`, `remoteMissingIsInconclusive`, `remoteFailureIsTechnical`, `simulatedHttpGetVerifiesAndPersistsCreationWithoutFinancialTransition`, `simulatedHttpErrors`, `duplicateOwnerContradicts`, `missingCurrencyIsInconclusive`, `concurrentFinancialApprovalIsPreserved`, `missingSnapshotIsInconclusive`, `amountMismatchContradicts`

### backend/src/test/java/com/jaldishop/backend/payment/application/preference/CheckoutRequestRetryTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`prepare`, `preferencesRetryIsEqualWhenInputsAndConfigurationStayTheSame`, `preferencesRetryChangesIfConfigurationChanges`, `ordersUseEachPreparedAttemptIdForTheSamePayment`, `ordersRetryChangesExpirationWhileKeepingReferenceAndKey`

### backend/src/test/java/com/jaldishop/backend/payment/application/preference/CreatePaymentPreferenceServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setInstant`, `advance`, `setUp`, `confirmedCreationRejectionIsSeparateFromFinancialStatusAndStoresNoBody`, `lateCreationErrorPreservesConcurrentFinancialApproval`, `shouldCreatePreferenceSuccessfullyForActiveReservation`, `shouldRejectReservationOfAnotherUser`, `shouldRejectExpiredReservation`, `shouldRejectWhenInventoryReservationIsExpired`, `shouldRejectCommittedReservation`, `shouldReturnExistingPreferenceOnRepeatedRequestWithoutDuplicateGatewayCall`, `shouldRejectWithConflictWhenAnotherRequestIsInFlight`, `shouldRecordTimeoutAndRecoverSameAttemptOnRetryWithoutDuplicateKey`, `shouldHandleDatabaseFailureAfterGatewaySuccessAndAllowIdempotentRecovery`, `shouldCreateOrderWhenCheckoutModeIsOrders`, `shouldReturnExistingOrderWhenAlreadyComplete`, `shouldHandleTimeoutAndReuseIdempotencyKeyInOrdersMode`, `shouldCalculateExpirationTimeForNewlyProtectedReservation`, `shouldCalculateExpirationTimeForPartiallyConsumedReservation`, `shouldRejectOrderCreationWhenReservationRemainingTimeNearExpiration`, `shouldRejectOrderCreationWhenReservationAlreadyExpired`, `shouldDetectClockAdvanceDuringPreparationAndRejectOrder`, `shouldNotIncludeExpirationTimeInPreferencesMode`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/ManualOrderReconciliationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`manualCallWorksWithSchedulersDisabledAndNeverSelectsBatchCandidates`, `consolidationUsesTimeAfterRemoteCallAndCleansUpEvenOnValidationFailure`, `remoteFailurePreservesFinancialStatusAndReleasesOnlyRequestedAttempt`, `alreadyApprovedWithoutOrderIsNotCompleteSuccessAndDoesNotQueryProvider`, `failedIncidencePersistenceNeverClaimsAnIncidenceWasRecorded`, `batchContinuesIfOneCandidatesLeaseCleanupFails`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`startMockHttpServer`, `handle`, `stopMockHttpServer`, `configureIsolatedProperties`, `manualApprovalAndDuplicateAffectOnlyTheSelectedAttemptWithSchedulersDisabled`, `failedIncidenceTransactionRollsBackRefundAndCanBeRetriedWithoutAnotherRemoteRead`, `cleanTestData`, `createCartWithItem`, `shouldRecoverAccreditedOrderWithoutWebhookAndConfirmPurchase`, `shouldKeepOpenCreatedOrderWithoutTransactions`, `shouldMarkAttemptRejectedOnCancelledOrderWithoutTransactions`, `shouldMarkAttemptRejectedOnExpiredOrderWithoutTransactions`, `shouldMarkAttemptRejectedOnCanceledOrderWithoutTransactions`, `shouldNotDegradeAlreadyApprovedPaymentWhenOrderCanceledRemote`, `shouldRecordIncidenceWhenAccreditedAfterCapacityReservationExpired`, `shouldAvoidDuplicateConfirmationWhenWebhookConcurrentlyApproves`, `shouldAllowOnlyOneWorkerToClaimAttemptConcurrently`, `shouldAllowNewWorkerToClaimExpiredLease`, `shouldRejectConsolidationFromStaleWorkerWithInvalidToken`, `shouldPreserveStateAndReleaseLeaseOnTimeoutOrHttp500`, `shouldRejectAmbiguousOrderWhenMultipleTransactionsReturned`, `shouldRecoverAfterWorkerCrashAndLeaseExpiration`, `shouldEnsureNoRegressionInApprovedPaymentsReconciliationAndRefunds`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/OrderReferenceReadTest.java

Tipo: ParameterizedTest; unitaria/slice candidata.

`emptyOrderWithValidReferencePreservesPaymentAndAppliesOnlyExpectedAttemptState`, `emptyOrderWithMissingMalformedOrCrossedReferenceCannotChangeFinancialState`, `concurrentlyApprovedPaymentRequiresValidCorrelationBeforeReleasingLease`, `contradictoryOrderOrMultipleTransactionsAreRejectedEvenWithValidReference`, `siblingAttemptReferenceCannotConsolidateClaimedOrder`, `missingRemoteResponseCannotBypassCorrelationForApprovedPayment`, `expiredLeaseCannotBeRevivedByPassingTheOldRequestTime`, `cleanupReleasesExpiredOwnLeaseButNeverASuccessorsLease`, `claimRoutesFirstThenReloadsUnderCanonicalPaymentAndAttemptLocks`, `pendingTransactionIsReportedAsOpenRatherThanCancelledOrExpired`, `reconciliationUsesTheSameAccountAndEnvironmentValidatorAsWebhook`, `approvedRemoteTransactionStillRequiresMatchingAmountAndCurrency`, `doBegin`, `doCommit`, `doRollback`, `assertRejected`, `consolidationValidatesReferenceUnderPaymentThenAttemptLocks`, `doBegin`, `doCommit`, `doRollback`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentConfirmationRunnerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldSkipConfirmationIfPaymentIsNotApproved`, `shouldSkipConfirmationIfPaymentHasPendingRefund`, `shouldSkipConfirmationIfIncidenceRequiresManualReview`, `shouldSkipConfirmationIfIncidenceRetryIsStillInTheFuture`, `shouldSkipConfirmationIfIncidenceAttemptsCountExhausted`, `shouldReturnExistingConfirmationIfOrderAlreadyExists`, `shouldExecuteConfirmPurchaseServiceSuccessfully`, `shouldPropagateConflictExceptionForRollback`, `shouldPropagateTransientExceptionForRollback`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentIncidenceRecorderTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldRecordDefinitiveCommercialFailureAndRequestRefund`, `duplicateCommercialFailureDoesNotCreateAnotherIncidenceOrRefund`, `incidenceStorageFailureMustPropagate`, `pendingRefundWithoutDurableCommercialEvidenceIsNotReportedAsRecorded`, `shouldRecordIndeterminateConflictWithoutRequestingRefund`, `shouldRecordTechnicalRetryWithoutRefund`, `shouldEscalateToManualReviewOnMaxAttempts`, `integrityFailureMustAbortInsteadOfClaimingARecordedRetry`, `shouldMarkActiveIncidenceResolved`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureIsolatedPostgres`, `setUp`, `shouldHandleTwoWorkersCreatingSameIncidenceConcurrently`, `shouldIncrementAttemptsCountConcurrentlyWithoutLostUpdates`, `shouldAvoidDuplicateOrdersWhenTwoReconcilersCompete`, `shouldRollbackOrderAndPersistCommercialFailureWhenReservationExpired`, `shouldSkipPaymentWhenReattemptHasBackoffStillActive`, `shouldSkipPaymentWhenIncidenceIsManualReviewRequired`, `shouldConfirmPurchaseWithoutDoubleInventoryDeductionOnReconciliation`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationProcessorTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`suppressedIncidenceIsNeverReportedAsRecordedCommercialFailure`, `setUp`, `shouldProcessSuccessfullyAndResolveIncidence`, `shouldHandleDefinitiveCommercialFailure`, `shouldHandleIndeterminateConflict`, `shouldHandleTransientDataAccessException`, `shouldHandleUnexpectedRuntimeException`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationSchedulerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`shouldTriggerReconciliationService`, `shouldCatchAndLogUnexpectedExceptions`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnEmptySummaryWhenNoCandidatesFound`, `shouldProcessBatchAndContinueEvenIfSingleCandidateFailsUncaught`

### backend/src/test/java/com/jaldishop/backend/payment/application/reconciliation/SingleAttemptIsolatedReconciliationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureIsolatedProperties`, `shouldReconcileTargetAttemptWithoutTouchingResidualAttempt`

### backend/src/test/java/com/jaldishop/backend/payment/application/refund/PaymentRefundPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`startMockHttpServer`, `handle`, `handle`, `stopMockHttpServer`, `configureIsolatedProperties`, `cleanTestData`, `shouldExecuteSuccessfulRefundWithMockHttp`, `shouldReconcileAfterTimeoutWhenGetReportsApproved`, `shouldProceedToPostWithSameKeyWhenGetReturnsEmpty`, `shouldHandleConcurrentWorkersWithoutRaceCondition`, `shouldRejectStaleWorkerConsolidationWhenLeaseWasTakenByAnotherWorker`, `shouldReuseSameIdempotencyKeyAcrossRetries`, `shouldHandleDefinitiveRejectionAndEscalateToManualReview`, `shouldReconcileSuccessfullyIfExternalSucceededBeforeLocalFailure`, `shouldPreventDuplicateRefundIntentsInDatabase`, `shouldEnsureNoTransactionIsOpenDuringHttpCall`, `shouldExcludeAndNotExecutePostWhenIncidenceIsManualReviewRequired`, `shouldExcludeAndNotExecutePostWhenIncidenceIsInFutureBackoff`, `shouldExcludeAndNotExecutePostWhenMaxAttemptsReached`, `shouldSuspendOperationallyAndNotFailWhenHttp401Or403`, `shouldTreatHttp409AsUnknownAndReconcileViaGet`, `shouldTreatUnrecognizedHttp400AsUnknownAndNotFailed`, `shouldExecuteSuccessfulRefundInOrdersModeWithPostgresAndMockHttp`

### backend/src/test/java/com/jaldishop/backend/payment/application/refund/PaymentRefundProcessorTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldClaimAndCreateRefundIntent`, `shouldReconcileViaGetBeforePostWhenPreviousStatusIsUnknown`, `shouldExecutePostWhenGetReportsEmpty`, `shouldRejectConsolidationIfClaimTokenLost`, `shouldConsolidateSuccessfullyWhenClaimIsValid`, `shouldSkipClaimWhenIncidenceIsInManualReviewRequired`, `shouldSkipClaimWhenIncidenceHasFutureBackoff`, `shouldSkipClaimWhenIncidenceReachedMaxAttempts`, `shouldSuspendOperationallyAndNotFailWhenMercadoPagoReturns401`, `shouldTreatHttp409AsIndeterminateUnknown`, `shouldTreatUnrecognizedHttp400AsIndeterminateUnknown`, `shouldTreatRecognizedHttp400AsDefinitiveCommercialFailure`, `shouldAllowRefundWhenOrderIsCancelled`, `shouldRejectRefundWhenOrderIsActiveConfirmed`, `shouldRejectRefundWhenOrderIsActiveInPreparation`, `shouldClaimAndCreateRefundIntentForOrdersMode`, `shouldReconcileViaGetOrderBeforePostWhenPreviousStatusIsUnknownForOrdersMode`, `shouldExecuteRefundPassingOrdersModeAndOrderId`

### backend/src/test/java/com/jaldishop/backend/payment/application/refund/PaymentRefundSchedulerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`shouldTriggerRefundServiceWithConfiguredBatchSize`, `defaultConstructorShouldUseBatchSize25`, `shouldCatchAndLogUnexpectedExceptions`

### backend/src/test/java/com/jaldishop/backend/payment/application/refund/PaymentRefundServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`shouldProcessPendingRefundsBatch`

### backend/src/test/java/com/jaldishop/backend/payment/application/refund/PaymentSchedulerExecutionPostgresIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`startMockHttpServer`, `handle`, `stopMockHttpServer`, `configureIsolatedProperties`, `cleanTestData`, `whenSchedulersDisabled_recordsRemainUntouched`, `whenRefundSchedulerExecutes_processesPendingBatchSuccessfully`, `whenRefundSchedulerEncountersTimeout_handlesResilientlyWithoutDuplicates`, `whenReconciliationSchedulerExecutes_runsSafelyRespectingBatchLimit`

### backend/src/test/java/com/jaldishop/backend/payment/application/webhook/OrdersWebhookCorrelationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setup`, `ambiguousHistory`, `historicalOrderIdentity`, `duplicateAndLatePending`, `rejectedMayBeAccreditedButNotBecomePending`, `conflictingIdentifiers`, `unrelatedCancellation`, `remotePaymentOwnedByAnotherPayment`, `completedRefundPreserved`, `explicitAttemptIsExclusiveAndLocksPaymentFirst`, `eitherSiblingReferenceSelectsExactlyItsAttempt`, `explicitAttemptCannotFallBackToOrderOwner`, `missingExplicitAttemptCannotFallBack`, `crossedAttemptAndMalformedReferencesDoNotMutate`, `cancellationValidatesExplicitReferenceBeforeTerminalShortcut`, `explicitCancellationAndPaymentNotificationAreSupported`, `explicitAttemptRejectsRemotePaymentBelongingToSibling`

### backend/src/test/java/com/jaldishop/backend/payment/application/webhook/ProcessMercadoPagoWebhookServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldProcessApprovedPaymentWebhookSuccessfully`, `shouldProcessPendingPaymentWebhookSuccessfully`, `shouldProcessRejectedPaymentWebhookSuccessfully`, `shouldIgnoreOutOfOrderPendingEventIfPaymentAlreadyApproved`, `shouldRejectWebhookWhenAmountDiffers`, `shouldRejectWebhookWhenCurrencyDiffers`, `shouldRejectWhenSignatureIsInvalid`, `shouldIgnoreUnsupportedEventsGracefully`, `shouldNotMarkPaymentFailedWhenGatewayThrowsTimeoutOrError`, `shouldRejectInvalidExternalReference`, `shouldRejectWhenQueryParamAndBodyDataIdAreInconsistent`, `shouldAcceptWhenQueryParamAndBodyDataIdAreConsistent`, `shouldRejectWhenCollectorIdMismatchesExpectedAccount`, `shouldRejectWhenLiveModeMismatchesExpectedEnvironment`, `shouldAcquirePessimisticLockOnPaymentDuringProcessing`, `shouldTriggerCommercialConfirmationWhenPaymentIsApproved`, `shouldPreserveApprovedFinancialStateAndRecordIncidenceWhenCommercialConfirmationFails`, `shouldHandleDuplicateWebhookGracefullyWhenOrderAlreadyConfirmed`, `shouldRethrowTransientDatabaseExceptionWithoutRecordingRefund`, `shouldResolveRaceWhenDataIntegrityViolationOccursAndOrderExists`, `shouldRethrowWhenDataIntegrityViolationOccursAndNoOrderExists`, `shouldRethrowUnexpectedExceptionWithoutRecordingRefund`, `shouldRecordIncidenceAndPreserveApprovedWhenStockInsufficient`, `shouldProcessOrderWebhookSuccessfullyWhenPaymentApproved`, `shouldIgnoreOrderWithoutTransactions`, `shouldRejectOrderWebhookOnAmountMismatch`, `shouldRejectOrderWebhookOnInvalidExternalReference`, `shouldRejectOrderWebhookOnCollectorMismatch`, `shouldProcessOrderPendingPaymentWithoutApproving`, `shouldRecordIncidenceWhenOrderConfirmedWithExpiredReservation`, `shouldProcessOrderWebhookWhenPaymentProcessedAndAccredited`, `shouldNotApproveOrderWebhookWhenPaymentProcessedWithoutAccreditation`, `shouldRejectAttemptWhenOrderCanceledWithoutTransactions`

### backend/src/test/java/com/jaldishop/backend/payment/domain/model/PaymentAttemptTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreatePaymentAttempt`, `shouldRejectInvalidAttemptNumber`, `shouldRecordPreference`, `shouldRecordOrder`, `shouldRejectInvalidProviderOrderId`, `shouldRehydrateLegacyAttemptWithoutProviderOrderId`, `shouldMarkApproved`, `shouldMarkRejectedAndError`

### backend/src/test/java/com/jaldishop/backend/payment/domain/model/PaymentIncidenceTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateNewOpenIncidence`, `shouldCreateCommercialTerminalIncidence`, `shouldIncrementAttemptsAndApplyBackoff`, `shouldEscalateToManualReviewWhenMaxAttemptsReached`, `shouldMarkAsResolved`, `shouldSanitizeSensitiveDataInMessage`

### backend/src/test/java/com/jaldishop/backend/payment/domain/model/PaymentRefundIntentTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreateValidRefundIntent`, `shouldNotAllowClaimIfLeaseIsActive`, `shouldAllowClaimIfLeaseExpired`, `shouldCompleteRefundIntent`, `shouldFailRefundIntent`, `shouldMarkUnknownAndAllowReclaim`

### backend/src/test/java/com/jaldishop/backend/payment/domain/model/PaymentTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldCreatePendingPayment`, `shouldRejectInvalidAmountOrCurrency`, `shouldMarkApproved`, `shouldRequestRefundAndMaintainApprovedStatus`, `shouldRejectRefundExceedingTotalAmount`

### backend/src/test/java/com/jaldishop/backend/payment/domain/model/RemoteOrderCreationStateTest.java

Tipo: ParameterizedTest; unitaria/slice candidata.

`creationDoesNotApprovePayment`, `confirmedRejectionIsIndependentAndCannotBecomeCreation`, `incompleteResponseCannotVerifyCreation`, `persistenceMappingPreservesEveryState`, `legacyNullRemainsConservativeAndPreferencesUnaffected`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/client/mercadopago/CheckoutCreationRequestStoreTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`durableOrdersSnapshotMarksUncertaintyBeforeReturningBody`, `duplicateSnapshotCannotReturnBodyForAnotherPost`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/client/mercadopago/MercadoPagoRestClientAdapterTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`documentsConfirmedOrdersValidationRejectionWithoutPersistingResponse`, `persistsExactPreferenceBodyBeforeSending`, `persistsExactOrderBodyBeforeSending`, `doesNotSendWhenPersistenceRejectsDuplicate`, `setUp`, `shouldCreatePreferenceSuccessfully`, `shouldSendIdempotencyKeyHeaderWhenProvided`, `shouldQueryApprovedPaymentSuccessfully`, `shouldQueryPendingPaymentSuccessfully`, `shouldQueryRejectedPaymentSuccessfully`, `shouldHandle400BadRequest`, `shouldHandle401Unauthorized`, `shouldHandle429RateLimit`, `shouldHandle500ServerError`, `shouldHandleMissingRequiredFieldsInPreferenceResponse`, `shouldHandleMissingRequiredFieldsInPaymentResponse`, `shouldFailFastWhenAccessTokenIsMissing`, `shouldHandleTimeout`, `shouldNotExposeSensitiveDataInExceptions`, `shouldCreateOrderSuccessfully`, `shouldFailCreateOrderWhenResponseIncomplete`, `shouldHandle400BadRequestOnCreateOrder`, `shouldQueryOrderSuccessfullyWithPayments`, `shouldQueryOrderSuccessfullyWithEmptyTransactions`, `shouldExecuteRefundSuccessfullyForPreferencesMode`, `shouldExecuteRefundSuccessfullyForOrdersMode`, `shouldQueryOrderWithRefundsSuccessfully`, `shouldIncludeExpirationTimeInOrderRequestWhenPresent`, `shouldOmitExpirationTimeWhenNull`, `shouldNotIncludeExpirationTimeInPreferenceRequest`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/client/mercadopago/OrderCreationRejectionTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`onlyExactDocumentedValidationIsConfirmed`, `ambiguousAndSensitiveBodiesNeverBecomeEvidence`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/client/mercadopago/dto/MpOrderSerializationTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldSerializeMonetaryFieldsAsStringsInOrdersApi`, `shouldNotContainIdNorCurrencyIdInItems`, `shouldOmitLastNameWhenNull`, `shouldOmitLastNameWhenNormalizedFromEmptyString`, `shouldOmitLastNameWhenNormalizedFromWhitespace`, `shouldPreserveValidLastName`, `shouldOmitAreaCodeWhenNullInPhone`, `shouldOmitConfigWhenAllUrlsAreNull`, `shouldOmitConfigWhenAllUrlsAreEmptyStrings`, `shouldOmitConfigWhenAllUrlsAreWhitespace`, `shouldIncludeConfigWhenUrlsAreValid`, `shouldIncludeOnlyConfiguredUrlsInPartialCombination`, `shouldNotAlterLegacyPreferencesRequest`, `shouldSerializeExpirationTimeWhenProvided`, `shouldOmitExpirationTimeWhenNull`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/persistence/RemoteOrderCreationMigrationTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`preservesHistoricalRowsAndAuditsAtomicTransitions`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/persistence/adapter/JpaPaymentAttemptRepositoryAdapterTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldFindByProviderOrderId`, `shouldFindByPreferenceId`, `shouldSavePaymentAttempt`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/persistence/mapper/PaymentIncidencePersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapBidirectionally`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/persistence/mapper/PaymentPersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapPaymentDomainAndEntityBidirectionally`, `shouldMapPaymentEntityWithNullFulfillmentType`, `shouldMapPaymentAttemptDomainAndEntityBidirectionally`, `shouldMapPaymentAttemptWithProviderOrderIdRoundtrip`, `shouldHandleLegacyEntityWithNullProviderOrderId`

### backend/src/test/java/com/jaldishop/backend/payment/infrastructure/security/MercadoPagoWebhookSignatureValidatorTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `shouldValidateCorrectSignatureSuccessfully`, `shouldRejectInvalidSignatureHash`, `shouldRejectIncompleteSignatureHeader`, `shouldRejectMissingHeadersOrDataId`, `shouldRejectReplayAttackWithExpiredTimestamp`

### backend/src/test/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationSecurityTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`resetInvocations`, `unauthenticatedCannotReconcile`, `endpointMapsExactlyOneAttemptAndNeverPassesClientFinancialState`, `customerCannotInvestigate`, `merchantCannotInvestigate`, `adminCanInvestigate`

### backend/src/test/java/com/jaldishop/backend/payment/web/controller/MercadoPagoPreferenceControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreatePreferenceSuccessfully`, `shouldRejectInvalidRequestBody`, `shouldHandleDomainConflictException`

### backend/src/test/java/com/jaldishop/backend/payment/web/controller/MercadoPagoWebhookControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturn200OnValidWebhook`, `shouldReturn401OnInvalidSignature`, `shouldReturn400OnMissingDataId`, `shouldReturn409OnFinancialConflict`

## realtime

### backend/src/test/java/com/jaldishop/backend/realtime/application/service/RealtimeNotificationPublisherTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `publishOrderConfirmedSendsToStoreTopic`, `publishOrderStatusChangedSendsToCustomerQueue`

### backend/src/test/java/com/jaldishop/backend/realtime/infrastructure/listener/OrderDomainEventListenerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `onOrderConfirmedDelegatesToPublisher`, `onOrderStatusChangedDelegatesToPublisher`, `onOrderConfirmedHandlesPublisherExceptionGracefully`, `onOrderStatusChangedHandlesPublisherExceptionGracefully`

### backend/src/test/java/com/jaldishop/backend/realtime/infrastructure/listener/OrderDomainEventListenerTransactionalTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`eventIsPublishedWhenTransactionCommits`, `eventIsNotPublishedWhenTransactionRollsBack`, `statusChangeEventIsNotPublishedWhenTransactionRollsBack`, `doBegin`, `doCommit`, `doRollback`, `executeWithCommit`, `executeWithRollback`, `executeStatusChangeWithRollback`

### backend/src/test/java/com/jaldishop/backend/realtime/infrastructure/websocket/StompAuthChannelInterceptorTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `connectWithValidBearerTokenAuthenticatesSuccessfully`, `connectWithPasscodeBearerTokenAuthenticatesSuccessfully`, `connectWithoutAuthorizationHeaderThrowsException`, `subscribeStoreOrdersByStoreOwnerIsPermitted`, `subscribeStoreOrdersByNonOwnerThrowsAccessDenied`, `subscribeStoreOrdersByCustomerThrowsAccessDenied`, `subscribeUserQueueOrdersByCustomerIsPermitted`, `subscribeUserQueueOrdersByNonCustomerThrowsAccessDenied`, `subscribeUnauthorizedDestinationThrowsAccessDenied`, `sendCommandByClientIsBlocked`

### backend/src/test/java/com/jaldishop/backend/realtime/infrastructure/websocket/WebSocketStompRealIntegrationTest.java

Tipo: SpringBootTest; requiere revisar PostgreSQL/contexto real.

`configureTestDatabase`, `tearDownSessions`, `cleanUpTestData`, `shouldConnectSuccessfullyWithValidJwt`, `shouldRejectConnectWithInvalidJwt`, `shouldRejectConnectWithExpiredJwt`, `shouldRejectConnectWithoutJwt`, `shouldAllowPermittedOriginAndRejectUnauthorizedOrigin`, `shouldBlockClientSendCommand`, `shouldRejectSubscriptionToUnauthorizedDestinations`, `shouldIsolateEventsBetweenStores`, `shouldRejectMerchantSubscribingToAnotherStore`, `shouldIsolateEventsBetweenCustomers`, `shouldDeliverToMultipleSessionsOfSameCustomer`, `shouldDeliverRealStompFrameOnConfirmPurchase`, `shouldDeliverRealStompFrameOnChangeOrderStatus`, `shouldNotDeliverFrameWhenTransactionRollsBack`, `shouldNotDeliverDuplicateFrameOnIdempotentRetry`, `shouldRequireReAuthenticationAndReSubscriptionUponReconnect`, `shouldDemonstrateEphemeralBehaviorDuringDisconnection`, `handleFrame`, `seedOrderInPostgres`, `seedCartAndProtectedPayment`, `handleException`, `handleTransportError`, `handleFrame`

## review

### backend/src/test/java/com/jaldishop/backend/review/application/service/CheckReviewEligibilityServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnEligibleWhenHasCompletedOrderAndNoReview`, `shouldReturnAlreadyReviewedWithCurrentReview`

### backend/src/test/java/com/jaldishop/backend/review/application/service/CreateProductReviewServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCreateReviewSuccessfully`, `shouldThrowConflictWhenReviewAlreadyExists`, `shouldThrowBusinessRuleExceptionWhenNoCompletedOrder`, `shouldThrowNotFoundWhenProductDoesNotExist`

### backend/src/test/java/com/jaldishop/backend/review/application/service/DeleteProductReviewServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldDeleteOwnReviewSuccessfully`, `shouldThrowWhenProductNotFound`, `shouldThrowWhenReviewNotFoundOrBelongsToAnotherUser`

### backend/src/test/java/com/jaldishop/backend/review/application/service/UpdateProductReviewServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldUpdateReviewSuccessfully`, `shouldThrowNotFoundWhenReviewDoesNotExist`

### backend/src/test/java/com/jaldishop/backend/review/web/controller/CustomerReviewControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldCheckEligibilitySuccessfully`, `shouldCreateReviewSuccessfully`, `shouldReturn422WhenNotEligible`, `shouldUpdateReviewSuccessfully`, `shouldGetCustomerReviewsList`, `shouldDeleteReviewSuccessfully`, `shouldReturn404WhenDeletingNonExistentReview`

### backend/src/test/java/com/jaldishop/backend/review/web/controller/PublicProductReviewControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnPublicReviews`

## shared

### backend/src/test/java/com/jaldishop/backend/shared/domain/model/MoneyTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`testBasicCreation`, `testArithmeticOperations`, `testDifferentCurrenciesThrow`, `testComparisons`, `testMonetarySummaryMoneyBridge`

### backend/src/test/java/com/jaldishop/backend/shared/domain/model/SharedDomainModelTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`testCustomerSnapshot`, `testDeliveryAddressSnapshotCoordinates`, `testMonetarySummary`

### backend/src/test/java/com/jaldishop/backend/shared/exception/GlobalExceptionHandlerTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `shouldReturn405MethodNotAllowed`, `shouldReturn400MissingRequiredParameter`, `shouldReturn404NoResourceFound`, `shouldReturn400ValidationErrors`, `shouldReturn400TypeMismatchSanitized`, `shouldReturn400MalformedJson`, `shouldReturn400IllegalArgument`, `shouldReturn422BusinessRule`, `shouldReturn409Conflict`, `shouldReturn404ResourceNotFound`, `shouldReturn401BadCredentials`, `shouldReturn403AccessDenied`, `shouldReturn500GenericException`, `shouldReturn504OnMercadoPagoTimeout`, `shouldReturn429OnMercadoPagoRateLimit`, `shouldReturn500OnMercadoPagoConfigError`, `shouldReturn400OnMercadoPagoBadRequestWithoutLeakingBody`, `shouldReturn502OnMercadoPagoServerError`, `shouldReturn409OnOptimisticLocking`, `shouldReturn409OnCannotAcquireLock`, `shouldReturn409OnDataIntegrityUniqueViolation`, `shouldReturn500OnDataIntegrityNonUniqueViolation`

### backend/src/test/java/com/jaldishop/backend/shared/exception/RestAccessDeniedHandlerTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`handleShouldReturn403WithApiError`

### backend/src/test/java/com/jaldishop/backend/shared/exception/RestAuthenticationEntryPointTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`commenceShouldReturn401WithApiError`

### backend/src/test/java/com/jaldishop/backend/shared/util/MethodValidationTestSupport.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.



### backend/src/test/java/com/jaldishop/backend/shared/util/PhoneUtilsTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`normalizeNineDigitsPhone`, `normalizeElevenDigitsPhone`, `preserveAlreadyFormattedPhone`, `handleNullOrBlank`, `preserveInternationalPhone`

## store

### backend/src/test/java/com/jaldishop/backend/store/application/service/ChangeStoreStatusServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldSuspendActiveStore`, `shouldThrowWhenStoreAlreadySuspended`, `shouldThrowWhenStoreAlreadyActive`, `shouldActivateSuspendedStore`, `shouldThrowWhenStoreNotFound`

### backend/src/test/java/com/jaldishop/backend/store/application/service/CloseMyStoreServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `hardDeleteStoreWhenNoOperationalHistory`, `deactivateStoreWhenHasOperationalHistory`, `throwExceptionWhenStoreNotFound`, `cleanupStoreMediaAfterDeletionInOrder`, `storeMediaCleanupFailureDoesNotPreventStoreDeletion`

### backend/src/test/java/com/jaldishop/backend/store/application/service/CreateStoreServiceTest.java

Tipo: ExtendWith, ParameterizedTest; unitaria/slice candidata.

`setUp`, `createStoreSuccessfully`, `shouldThrowExceptionAndRollbackWhenMerchantRoleAssignmentFails`, `throwExceptionWhenMerchantAlreadyHasStore`, `throwExceptionWhenSlugAlreadyExists`, `generateSlugFromNameWhenSlugIsNull`, `shouldNormalizeUnicodeAccentsAndSymbolsWhenGeneratingSlug`, `shouldUseExplicitSlugWhenProvided`, `shouldThrowIllegalArgumentExceptionWhenSlugBecomesEmptyAfterNormalization`

### backend/src/test/java/com/jaldishop/backend/store/application/service/GetAdminStoresServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListStores`, `shouldGetStoreById`, `shouldThrowWhenStoreNotFound`

### backend/src/test/java/com/jaldishop/backend/store/application/service/GetMyStoreServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `getMyStoreSuccessfully`, `throwExceptionWhenStoreNotFound`

### backend/src/test/java/com/jaldishop/backend/store/application/service/GetStoreBySlugServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`shouldReturnActiveStoreWhenSlugExists`, `shouldThrowNotFoundWhenSlugDoesNotExist`, `shouldThrowNotFoundWhenStoreIsInactive`, `shouldThrowIllegalArgumentWhenSlugInvalid`

### backend/src/test/java/com/jaldishop/backend/store/application/service/GetStoreCategoriesServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`executeShouldReturnAllActiveCategories`

### backend/src/test/java/com/jaldishop/backend/store/application/service/GetStoreCustomersServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnCustomersList`, `shouldThrowExceptionWhenStoreIdIsNull`

### backend/src/test/java/com/jaldishop/backend/store/application/service/RegisterStoreCustomerServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldRegisterCustomer`, `shouldDoNothingWhenIdsAreNull`

### backend/src/test/java/com/jaldishop/backend/store/application/service/StoreContextServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `requireStoreIdSuccess`, `requireStoreIdThrowsWhenNotMerchant`, `requireStoreIdThrowsWhenPrincipalNull`, `requireStoreSuccess`, `validateStoreOwnershipSuccess`, `validateStoreOwnershipThrowsWhenMismatch`

### backend/src/test/java/com/jaldishop/backend/store/application/service/StoreProvisioningAdapterTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldMapAllTwentyFieldsOneToOne`, `shouldDelegateToCreateStoreServiceAndReturnStoreId`, `shouldPropagateCreateStoreFailure`

### backend/src/test/java/com/jaldishop/backend/store/application/service/UpdateStoreServiceTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `updateStoreSuccessfully`, `throwExceptionWhenUpdatingNonExistentStore`

### backend/src/test/java/com/jaldishop/backend/store/domain/model/StoreCategoryTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createStoreCategorySuccessfully`, `updateAndChangeStatus`, `throwWhenInvalidFields`

### backend/src/test/java/com/jaldishop/backend/store/domain/model/StoreTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`createStoreWithValidValues`, `throwExceptionWhenMerchantUserIdIsNull`, `throwExceptionWhenNameIsNull`, `throwExceptionWhenNameIsBlank`, `throwExceptionWhenSlugIsNull`, `throwExceptionWhenSlugIsBlank`, `throwExceptionWhenDeliveryFeeIsNegative`, `allowDeliveryDisabledWithNullFee`, `allowTaxRateZero`, `throwExceptionWhenTaxRateIsNegative`, `throwExceptionWhenTaxRateExceeds100`, `updateStoreProfile`, `activateAndDeactivateStore`, `reconstituteStore`, `throwExceptionWhenOnlyOneCoordinateProvided`, `updateStorefrontDetails`, `suspendShouldChangeStatusAndThrowConflictWhenAlreadySuspended`, `activateShouldChangeStatusAndThrowConflictWhenAlreadyActive`

### backend/src/test/java/com/jaldishop/backend/store/domain/model/StoreValueObjectsTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`testStoreLocationCoordinates`, `testDeliveryConfigurationValidation`

### backend/src/test/java/com/jaldishop/backend/store/infrastructure/persistence/mapper/StorePersistenceMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`setUp`, `toEntityShouldMapAllFields`, `toDomainShouldMapAllFields`, `shouldHandleNullableFieldsBothWays`

### backend/src/test/java/com/jaldishop/backend/store/web/controller/AdminStoreControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldListStores`, `shouldGetStoreById`, `shouldSuspendStore`, `shouldActivateStore`, `listStoresInvalidQueryParameter`, `shouldReturn409WhenStoreAlreadySuspended`, `shouldReturn409WhenStoreAlreadyActive`

### backend/src/test/java/com/jaldishop/backend/store/web/controller/MerchantCustomerControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `shouldReturnCustomersList`, `listCustomersInvalidQueryParameter`

### backend/src/test/java/com/jaldishop/backend/store/web/controller/StoreCategoryControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `getStoreCategoriesSuccess`

### backend/src/test/java/com/jaldishop/backend/store/web/controller/StoreControllerTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `createStoreSuccess`, `createStoreSuccessWhenCustomer`, `createStoreValidationErrorWhenNameIsBlank`, `getMyStoreSuccess`, `updateMyStoreSuccess`, `getStoreBySlugSuccess`, `closeMyStoreHardDeleted`, `closeMyStoreDeactivated`, `searchAndSlugInvalidParameters`

### backend/src/test/java/com/jaldishop/backend/store/web/mapper/AdminStoreResponseMapperTest.java

Tipo: ExtendWith; unitaria/slice candidata.

`setUp`, `toSummaryShouldMapCorrectly`, `toDetailShouldMapCorrectly`

### backend/src/test/java/com/jaldishop/backend/store/web/mapper/StoreCustomerResponseMapperTest.java

Tipo: sin anotación de contexto identificada; unitaria/slice candidata.

`shouldMapAllNineFields`, `shouldPreserveNullSemantics`

## testutil

### backend/src/test/java/com/jaldishop/backend/testutil/PostgresTriggerAssertions.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`assertFailure`

### backend/src/test/java/com/jaldishop/backend/testutil/PostgresTriggerAssertionsTest.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`acceptsOnlyTheExpectedSqlStateAndMessageInTheCauseChain`

### backend/src/test/java/com/jaldishop/backend/testutil/TestPostgresConfig.java

Tipo: sin anotación de contexto identificada; requiere revisar PostgreSQL/contexto real.

`requireDestructiveIntegrationDatabase`

### backend/src/test/java/com/jaldishop/backend/testutil/TestPostgresConfigTest.java

Tipo: ParameterizedTest; requiere revisar PostgreSQL/contexto real.

`separateEnvironmentUrlsCanCoexistWithoutFallbackOrCrossing`, `shouldAcceptValidLocalIntegrationTestUrl`, `normalSuitesKeepTheirOriginalDatabaseAndRejectPhase2aDatabase`, `shouldRejectInvalidOrUnsafeUrls`, `shouldRejectNullOrBlankCredentials`, `destructiveFixturesRequireExactIntegrationCatalog`

## Reportes históricos encontrados

Se leen únicamente atributos de testsuite. No se verificó fecha, relación con working tree, entorno ni reproducibilidad. No cuentan como ejecución aprobada de esta auditoría.

| Suite | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| com.jaldishop.backend.checkout.application.service.ConfirmPurchaseServiceTest | 18 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.gateway.MercadoPagoResourceValidatorTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.ManualOrderReconciliationTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.OrderReconciliationPostgresIntegrationTest | 16 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.OrderReferenceReadTest | 25 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.PaymentConfirmationRunnerTest | 9 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.PaymentIncidenceRecorderTest | 9 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationProcessorTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationSchedulerTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationServiceTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.webhook.OrdersWebhookCorrelationTest | 17 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookServiceTest | 33 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.domain.model.PaymentAttemptTest | 8 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.domain.model.PaymentIncidenceTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.payment.web.controller.AdminOrderInvestigationSecurityTest | 5 | 0 | 0 | 0 |
| com.jaldishop.backend.testutil.TestPostgresConfigTest | 14 | 0 | 0 | 0 |

## Resumen de estructura de tests por módulo

Las categorías se solapan; MockMvc standalone no demuestra filtros JWT reales. Contar archivos no equivale a cobertura de comportamientos.

| Módulo | Archivos *Test.java | SpringBootTest | MockMvc/WebMvc | Mockito | Referencia PostgreSQL/integración |
|---|---:|---:|---:|---:|---:|
| address | 6 | 0 | 1 | 5 | 0 |
| capacity | 22 | 0 | 5 | 15 | 0 |
| cart | 12 | 0 | 1 | 8 | 0 |
| catalog | 25 | 0 | 5 | 15 | 0 |
| checkout | 8 | 0 | 1 | 4 | 2 |
| database | 1 | 0 | 0 | 0 | 1 |
| e2e | 1 | 1 | 0 | 0 | 1 |
| favorite | 2 | 0 | 1 | 2 | 0 |
| identity | 16 | 1 | 4 | 11 | 1 |
| inventory | 10 | 2 | 1 | 5 | 3 |
| media | 4 | 0 | 1 | 4 | 0 |
| notification | 10 | 1 | 2 | 6 | 1 |
| ordering | 16 | 4 | 2 | 12 | 4 |
| payment | 43 | 8 | 3 | 23 | 9 |
| realtime | 5 | 1 | 0 | 4 | 1 |
| review | 6 | 0 | 2 | 6 | 0 |
| shared | 6 | 0 | 1 | 0 | 0 |
| store | 22 | 0 | 4 | 17 | 0 |
| testutil | 2 | 0 | 0 | 0 | 2 |

PostgreSQL real es necesario para validar migraciones, trigger tenant, FK compuestas, exclusión de solapes, locks, unique, leases, rollback financiero y carreras. Los mocks de repositorio no prueban estas propiedades. Realtime STOMP real requiere servidor/socket local en su fase autorizada. No se ejecutó ningún contexto.
