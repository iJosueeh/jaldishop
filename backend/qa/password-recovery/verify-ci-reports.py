#!/usr/bin/env python3
"""Fail CI on absent/failed/skipped reports or missing critical recovery scenarios."""
import argparse
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET

PREFIX = 'com.jaldishop.backend.'
CRITICAL = {
    PREFIX + 'identity.application.service.PasswordRecoveryPostgresIntegrationTest': {
        'fullFlowWorksForCustomerAndMerchantWithJwtRevocation': 2,
        'expiresExactlyAtFifteenMinutesAndUnknownTokensHaveSameError': 1,
        'newRequestReplacesPreviousToken': 1,
        'concurrentResetsHaveExactlyOneWinner': 1,
        'concurrentRequestsLeaveOnlyOneUsableToken': 1,
        'failedCredentialUpdateRollsBackTokenConsumption': 1,
        'staleProfileSaveCannotRestorePasswordOrRevokedSessions': 1,
        'additiveMigrationPreservesAllPreexistingCredentials': 1,
    },
    PREFIX + 'identity.infrastructure.security.PasswordRecoveryWebSocketIntegrationTest': {
        'oldJwtCannotConnectAfterPasswordReset': 1,
        'alreadySubscribedSessionStopsReceivingAfterPasswordReset': 1,
    },
    PREFIX + 'identity.infrastructure.security.PasswordRecoverySecurityWebTest': {
        'bothRecoveryEndpointsArePublicValidateAndNeverReturnSecrets': 1,
        'oldJwtCannotRefreshButNewJwtCanIncludingImmediatelyInSameSecond': 1,
        'legacyJwtWithoutVersionRemainsUsableUntilPasswordReset': 1,
        'ipLimitsCannotBeBypassedUsingForwardedHeaders': 1,
    },
    PREFIX + 'identity.application.service.ForgotPasswordServiceTest': {
        'normalizesAndQueuesWithoutAccountLookupAndSuppressesAccountLimitForAllAddresses': 1,
        'queueCapacityReturnsRateLimitAndProviderFailureDoesNotReachHttpCaller': 1,
    },
    PREFIX + 'realtime.infrastructure.websocket.StompAuthChannelInterceptorTest': {
        'passwordResetRejectsOldConnectAndExistingSubscription': 1,
    },
    PREFIX + 'realtime.infrastructure.websocket.StompDeliveryAuthorizationBrokerTest': {
        'passwordResetStopsDeliveryToAlreadySubscribedSessions': 1,
        'newSessionReceivesAfterResetWhileOldSessionRemainsConnected': 1,
    },
}


def verify(directory, maven_outcome, critical=False):
    problems = []
    if maven_outcome != 'success':
        problems.append('Maven did not succeed: ' + maven_outcome)
    files = sorted(Path(directory).glob('TEST-*.xml'))
    if not files:
        problems.append('No Surefire XML files found')
    counts = Counter()
    executed = Counter()
    for filename in files:
        root = ET.parse(filename).getroot()
        suites = [root] if root.tag == 'testsuite' else list(root.findall('testsuite'))
        for suite in suites:
            for key in ['tests', 'failures', 'errors', 'skipped']:
                counts[key] += int(suite.get(key, '0'))
        for case in root.iter('testcase'):
            blocked = any(case.find(tag) is not None for tag in ['failure', 'error', 'skipped', 'aborted'])
            if blocked:
                problems.append('Non-passing test: ' + case.get('classname', root.get('name', 'unknown')))
                continue
            classname = case.get('classname', root.get('name', ''))
            name = case.get('name', '')
            for method in CRITICAL.get(classname, {}):
                if name == method or name.startswith((method + '(', method + '[')):
                    executed[(classname, method)] += 1
    if counts['tests'] == 0:
        problems.append('Zero tests reported')
    if any(counts[key] for key in ['failures', 'errors', 'skipped']):
        problems.append('Non-passing Surefire counters: ' + str(dict(counts)))
    if critical:
        for classname, methods in CRITICAL.items():
            for method, minimum in methods.items():
                if executed[(classname, method)] < minimum:
                    problems.append('Critical scenario missing: ' + classname + '#' + method)
    return counts, problems


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory')
    parser.add_argument('--maven-outcome', required=True)
    parser.add_argument('--critical', action='store_true')
    args = parser.parse_args()
    counts, problems = verify(args.directory, args.maven_outcome, args.critical)
    print('Surefire totals:', dict(counts))
    for problem in sorted(set(problems)):
        print('::error::' + problem)
    return 1 if problems else 0


if __name__ == '__main__':
    raise SystemExit(main())
