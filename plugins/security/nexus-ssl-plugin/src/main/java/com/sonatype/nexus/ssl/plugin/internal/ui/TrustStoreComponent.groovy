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

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import com.sonatype.nexus.ssl.model.CertificatePemXO
import com.sonatype.nexus.ssl.model.CertificateXO
import com.sonatype.nexus.ssl.plugin.internal.rest.TrustStoreResource

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.hibernate.validator.constraints.NotEmpty

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

  // FIXME: Do not inject a REST endpoint component as a service
  @Inject
  TrustStoreResource trustStoreResource

  /**
   * Retrieves certificates.
   * @return a list of certificates
   */
  @DirectMethod
  List<CertificateXO> read() {
    return trustStoreResource.get().entity
  }

  /**
   * Creates a certificate.
   * @param pem to be created
   * @return created certificate
   */
  @DirectMethod
  @Validate(groups = [Create.class, Default.class])
  CertificateXO create(final @NotNull @Valid CertificatePemXO pem) {
    return trustStoreResource.create(pem)
  }

  /**
   * Deletes a certificate.
   * @param id of certificate to be deleted
   */
  @DirectMethod
  @RequiresAuthentication
  @Validate
  void remove(final @NotEmpty String id) {
    trustStoreResource.delete(id)
  }

}
