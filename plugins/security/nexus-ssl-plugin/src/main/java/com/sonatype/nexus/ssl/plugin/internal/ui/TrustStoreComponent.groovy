/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package com.sonatype.nexus.ssl.plugin.internal.ui

import java.security.cert.Certificate
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.naming.ldap.LdapName
import javax.naming.ldap.Rdn
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import com.sonatype.nexus.ssl.plugin.TrustStore

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.ValidationMessage
import org.sonatype.nexus.validation.ValidationResponse
import org.sonatype.nexus.validation.ValidationResponseException
import org.sonatype.nexus.validation.group.Create
import org.sonatype.sisu.goodies.ssl.keystore.CertificateUtil

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static org.sonatype.sisu.goodies.ssl.keystore.CertificateUtil.calculateFingerprint

/**
 * SSL TrustStore {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'ssl_TrustStore')
class TrustStoreComponent
extends DirectComponentSupport
{

  @Inject
  TrustStore trustStore

  /**
   * Retrieves certificates.
   * @return a list of certificates
   */
  @DirectMethod
  @RequiresPermissions('nexus:ssl:truststore:read')
  List<CertificateXO> read() {
    return trustStore.trustedCertificates?.collect { certificate ->
      asCertificateXO(certificate, true)
    }
  }

  /**
   * Creates a certificate.
   * @param pem to be created
   * @return created certificate
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:ssl:truststore:create')
  @Validate(groups = [Create.class, Default.class])
  CertificateXO create(final @NotNull @Valid CertificatePemXO pem) {
    try {
      Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(pem.value)
      trustStore.importTrustCertificate(certificate, calculateFingerprint(certificate))
      return asCertificateXO(certificate, true)
    }
    catch (CertificateParsingException e) {
      ValidationResponse validations = new ValidationResponse()
      validations.addError(new ValidationMessage('pem', 'Invalid PEM formatted certificate'))
      throw new ValidationResponseException(validations)
    }
  }

  /**
   * Deletes a certificate.
   * @param id of certificate to be deleted
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:ssl:truststore:delete')
  @Validate
  void remove(final @NotEmpty String id) {
    trustStore.removeTrustCertificate(id)
  }

  @PackageScope
  static CertificateXO asCertificateXO(final Certificate certificate, final boolean inTrustStore) {
    String fingerprint = calculateFingerprint(certificate)

    if (certificate instanceof X509Certificate) {
      X509Certificate x509Certificate = (X509Certificate) certificate

      Map<String, String> subjectRdns = getRdns(x509Certificate.subjectX500Principal.name)
      Map<String, String> issuerRdns = getRdns(x509Certificate.issuerX500Principal.name)

      return new CertificateXO(
          id: fingerprint,
          pem: CertificateUtil.serializeCertificateInPEM(certificate),
          fingerprint: fingerprint,
          serialNumber: x509Certificate.serialNumber,
          subjectCommonName: subjectRdns.get("CN"),
          subjectOrganization: subjectRdns.get("O"),
          subjectOrganizationalUnit: subjectRdns.get("OU"),
          issuerCommonName: issuerRdns.get("CN"),
          issuerOrganization: issuerRdns.get("O"),
          issuerOrganizationalUnit: issuerRdns.get("OU"),
          issuedOn: x509Certificate.notBefore.time,
          expiresOn: x509Certificate.notAfter.time,
          inTrustStore: inTrustStore
      )
    }
    else {
      return new CertificateXO(
          id: fingerprint,
          pem: CertificateUtil.serializeCertificateInPEM(certificate),
          fingerprint: fingerprint
      )
    }
  }

  private static Map<String, String> getRdns(final String dn) {
    Map<String, String> rdns = [:]
    LdapName ldapName = new LdapName(dn)
    for (Rdn rdn : ldapName.rdns) {
      rdns.put(rdn.type, rdn.value.toString())
    }
    return rdns
  }

}
