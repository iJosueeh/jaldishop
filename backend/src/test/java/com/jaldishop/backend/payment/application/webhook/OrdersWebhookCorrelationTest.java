package com.jaldishop.backend.payment.application.webhook;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrdersWebhookCorrelationTest {
 final UUID id=UUID.randomUUID();
 final Payment p=mock(Payment.class);
 final PaymentRepository ps=mock(PaymentRepository.class);
 final PaymentAttemptRepository as=mock(PaymentAttemptRepository.class);
 final PaymentWebhookPersistenceService s=new PaymentWebhookPersistenceService(ps,as);
 PaymentAttempt attempt(int n,String order) {
  var a=PaymentAttempt.create(id,n,PaymentProvider.MERCADO_PAGO,PaymentMethod.CARD,"key"+n,"ORDERS");
  a.recordOrder(order,"https://checkout.test");
  when(as.findByIdForUpdate(a.getId())).thenReturn(Optional.of(a)); return a;
 }
 void setup(PaymentAttempt... a) {
  when(ps.findByIdForUpdate(id)).thenReturn(Optional.of(p));
  when(p.getAmount()).thenReturn(BigDecimal.TEN); when(p.getCurrency()).thenReturn("PEN");
  when(p.getStatus()).thenReturn(PaymentStatus.PENDING);
  when(as.findByPaymentIdOrderByAttemptNumberAsc(id)).thenReturn(List.of(a));
 }
 PaymentQueryResult result(String status) {
  return new PaymentQueryResult("pay1",status,"accredited",BigDecimal.TEN,"PEN","visa","credit_card",id.toString(),null,null,null);
 }
 @Test void ambiguousHistory() {
  setup(attempt(1,"one"),attempt(2,"two"));
  assertThrows(ConflictException.class,()->s.applyPaymentQueryResult(id,result("approved"),"unknown"));
  verify(as,never()).save(any());
 }
 @Test void historicalOrderIdentity() {
  var a=attempt(1,"one"); var b=attempt(2,"two"); setup(a,b);
  s.applyPaymentQueryResult(id,result("approved"),"one");
  assertEquals(PaymentAttemptStatus.APPROVED,a.getStatus()); assertEquals(PaymentAttemptStatus.CREATED,b.getStatus());
  var locks=inOrder(ps,as); locks.verify(ps).findByIdForUpdate(id); locks.verify(as).findByIdForUpdate(a.getId());
 }
 @Test void duplicateAndLatePending() {
  var a=attempt(1,"one"); setup(a);
  s.applyPaymentQueryResult(id,result("approved"),"one"); var completed=a.getCompletedAt();
  s.applyPaymentQueryResult(id,result("approved"),"one"); s.applyPaymentQueryResult(id,result("pending"),"one");
  assertEquals(completed,a.getCompletedAt()); assertEquals(PaymentAttemptStatus.APPROVED,a.getStatus()); verify(as,times(1)).save(a);
 }
 @Test void rejectedMayBeAccreditedButNotBecomePending() {
  var a=attempt(1,"one"); setup(a); s.applyPaymentQueryResult(id,result("rejected"),"one");
  s.applyPaymentQueryResult(id,result("pending"),"one"); assertEquals(PaymentAttemptStatus.REJECTED,a.getStatus());
  s.applyPaymentQueryResult(id,result("approved"),"one"); assertEquals(PaymentAttemptStatus.APPROVED,a.getStatus());
 }
 @Test void conflictingIdentifiers() {
  var a=attempt(1,"one"); var b=attempt(2,"two"); setup(a,b); a.markProviderPending("pay1","pending",null,null);
  assertThrows(ConflictException.class,()->s.applyPaymentQueryResult(id,result("approved"),"two")); verify(as,never()).save(any());
 }
 @Test void unrelatedCancellation() {
  var a=attempt(1,"one"); setup(a); assertThrows(ConflictException.class,()->s.applyOrderCancellation(id,"other","expired",null));
  assertEquals(PaymentAttemptStatus.CREATED,a.getStatus());
 }
 @Test void remotePaymentOwnedByAnotherPayment() {
  var a=attempt(1,"one"); setup(a);
  var other=PaymentAttempt.create(UUID.randomUUID(),1,PaymentProvider.MERCADO_PAGO,PaymentMethod.CARD,"other","ORDERS");
  when(as.findByProviderAndProviderPaymentId(PaymentProvider.MERCADO_PAGO,"pay1")).thenReturn(Optional.of(other));
  assertThrows(ConflictException.class,()->s.applyPaymentQueryResult(id,result("approved"),"one"));
  verify(as,never()).save(any());
 }
 @Test void completedRefundPreserved() {
  when(ps.findByIdForUpdate(id)).thenReturn(Optional.of(p)); when(p.getStatus()).thenReturn(PaymentStatus.APPROVED);
  when(p.getRefundStatus()).thenReturn(RefundStatus.COMPLETED); s.recordApprovalIncidence(id,"late"); verify(ps,never()).save(any());
 }

 PaymentQueryResult reference(String value) {
  return new PaymentQueryResult("pay1","approved","accredited",BigDecimal.TEN,"PEN","visa","credit_card",value,null,null,null);
 }
 @Test void explicitAttemptIsExclusiveAndLocksPaymentFirst() {
  var a=attempt(1,"one"); var b=attempt(2,"two"); setup(a,b);
  when(as.findById(a.getId())).thenReturn(Optional.of(a));
  s.applyPaymentQueryResult(id,reference("pa1_"+a.getId()),"one");
  assertEquals(PaymentAttemptStatus.APPROVED,a.getStatus());
  assertEquals(PaymentAttemptStatus.CREATED,b.getStatus());
  var locks=inOrder(ps,as); locks.verify(ps).findByIdForUpdate(id); locks.verify(as).findByIdForUpdate(a.getId());
 }
 @Test void explicitAttemptCannotFallBackToOrderOwner() {
  var a=attempt(1,"one"); var b=attempt(2,"two"); setup(a,b);
  when(as.findById(b.getId())).thenReturn(Optional.of(b));
  assertThrows(ConflictException.class,()->s.applyPaymentQueryResult(id,reference("pa1_"+b.getId()),"one"));
  verify(ps,never()).save(any()); verify(as,never()).save(any());
 }
 @Test void missingExplicitAttemptCannotFallBack() {
  setup(attempt(1,"one"));
  assertThrows(com.jaldishop.backend.shared.exception.ResourceNotFoundException.class,
   ()->s.applyPaymentQueryResult(id,reference("pa1_"+UUID.randomUUID()),"one"));
  verify(ps,never()).save(any()); verify(as,never()).save(any());
 }
 @Test void crossedAttemptAndMalformedReferencesDoNotMutate() {
  setup(attempt(1,"one"));
  var foreign=PaymentAttempt.create(UUID.randomUUID(),1,PaymentProvider.MERCADO_PAGO,PaymentMethod.CARD,"foreign","ORDERS");
  when(as.findById(foreign.getId())).thenReturn(Optional.of(foreign));
  for(String ref:List.of("pa1_"+foreign.getId(),"pa1_bad",UUID.randomUUID().toString()))
   assertThrows(ConflictException.class,()->s.applyPaymentQueryResult(id,reference(ref),"one"));
  verify(ps,never()).save(any()); verify(as,never()).save(any());
 }
 @Test void cancellationValidatesExplicitReferenceBeforeTerminalShortcut() {
  var a=attempt(1,"one"); var b=attempt(2,"two"); setup(a,b);
  when(p.getStatus()).thenReturn(PaymentStatus.APPROVED);
  assertThrows(ConflictException.class,()->s.applyOrderCancellation(id,"one","expired",null,"pa1_"+b.getId()));
  verify(ps,never()).save(any()); verify(as,never()).save(any());
 }

 @Test void explicitCancellationAndPaymentNotificationAreSupported() {
  var a=attempt(1,"one"); setup(a);
  when(as.findById(a.getId())).thenReturn(Optional.of(a));
  assertEquals(id,s.resolvePaymentId(com.jaldishop.backend.payment.application.gateway.ExternalPaymentReference.parse("pa1_"+a.getId()).orElseThrow()));
  s.applyOrderCancellation(id,"one","expired",null,"pa1_"+a.getId());
  assertEquals(PaymentAttemptStatus.REJECTED,a.getStatus());
  s.applyPaymentQueryResult(id,reference("pa1_"+a.getId()),null);
  assertEquals(PaymentAttemptStatus.APPROVED,a.getStatus());
 }
 @Test void explicitAttemptRejectsRemotePaymentBelongingToSibling() {
  var a=attempt(1,"one"); var b=attempt(2,"two"); setup(a,b);
  when(as.findById(a.getId())).thenReturn(Optional.of(a));
  when(as.findByProviderAndProviderPaymentId(PaymentProvider.MERCADO_PAGO,"pay1")).thenReturn(Optional.of(b));
  assertThrows(ConflictException.class,()->s.applyPaymentQueryResult(id,reference("pa1_"+a.getId()),"one"));
  verify(ps,never()).save(any()); verify(as,never()).save(any());
 }
}
