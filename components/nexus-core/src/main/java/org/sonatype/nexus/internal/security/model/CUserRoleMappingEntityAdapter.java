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
package org.sonatype.nexus.internal.security.model;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link CUserRoleMapping} entity adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class CUserRoleMappingEntityAdapter
    extends ComponentSupport
{
  public static final String DB_CLASS = new OClassNameBuilder()
      .prefix("security")
      .type("user_role_mapping")
      .build();

  public static final String P_USER_ID = "userId";

  public static final String P_SOURCE = "source";

  public static final String P_ROLES = "roles";

  private static final String I_USER_ID_SOURCE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_USER_ID)
      .property(P_SOURCE)
      .build();

  /**
   * Register schema.
   */
  public OClass register(final ODatabaseDocumentTx db, final Runnable initializer) {
    checkNotNull(db);

    OSchema schema = db.getMetadata().getSchema();
    OClass type = schema.getClass(DB_CLASS);
    if (type == null) {
      type = schema.createClass(DB_CLASS);

      type.createProperty(P_USER_ID, OType.STRING).setNotNull(true);
      type.createProperty(P_SOURCE, OType.STRING).setNotNull(true);
      type.createProperty(P_ROLES, OType.EMBEDDEDSET);

      type.createIndex(I_USER_ID_SOURCE, INDEX_TYPE.UNIQUE, P_USER_ID, P_SOURCE);

      log.info("Created schema: {}, properties: {}", type, type.properties());

      initializer.run();
    }
    return type;
  }

  /**
   * Create a new document and write entity.
   */
  public ODocument create(final ODatabaseDocumentTx db, final CUserRoleMapping entity) {
    checkNotNull(db);
    checkNotNull(entity);

    ODocument doc = db.newInstance(DB_CLASS);
    return write(doc, entity);
  }

  /**
   * Write entity to document.
   */
  public ODocument write(final ODocument document, final CUserRoleMapping entity) {
    checkNotNull(document);
    checkNotNull(entity);

    document.field(P_USER_ID, entity.getUserId());
    document.field(P_SOURCE, entity.getSource());
    document.field(P_ROLES, entity.getRoles());

    return document.save();
  }

  /**
   * Read entity from document.
   */
  public CUserRoleMapping read(final ODocument document) {
    checkNotNull(document);

    CUserRoleMapping entity = new CUserRoleMapping();
    entity.setUserId(document.<String>field(P_USER_ID, OType.STRING));
    entity.setSource(document.<String>field(P_SOURCE, OType.STRING));
    entity.setRoles(Sets.newHashSet(document.<Set<String>>field(P_ROLES, OType.EMBEDDEDSET)));

    entity.setVersion(String.valueOf(document.getVersion()));

    return entity;
  }

  /**
   * Browse all documents.
   */
  public Iterable<ODocument> browse(final ODatabaseDocumentTx db) {
    checkNotNull(db);
    return db.browseClass(DB_CLASS);
  }

  /**
   * Get all user role mappings.
   */
  public Iterable<CUserRoleMapping> get(final ODatabaseDocumentTx db) {
    return Iterables.transform(browse(db), new Function<ODocument, CUserRoleMapping>()
    {
      @Nullable
      @Override
      public CUserRoleMapping apply(@Nullable final ODocument input) {
        return input == null ? null : read(input);
      }
    });
  }

  /**
   * Retrieves a mapping document.
   *
   * @return found document, null otherwise
   */
  @Nullable
  public ODocument get(final ODatabaseDocumentTx db, final String userId, final String source) {
    OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
        "SELECT FROM " + DB_CLASS + " WHERE " + P_USER_ID + " = ?" + " AND " + P_SOURCE + " = ?"
    );
    List<ODocument> results = db.command(query).execute(userId, source);
    if (results.isEmpty()) {
      return null;
    }
    return results.get(0);
  }

  /**
   * Deletes a mapping.
   *
   * @return true if mapping was deleted
   */
  public boolean delete(final ODatabaseDocumentTx db, final String userId, final String source) {
    OCommandSQL command = new OCommandSQL(
        "DELETE FROM " + DB_CLASS + " WHERE " + P_USER_ID + " = ?" + " AND " + P_SOURCE + " = ?"
    );
    int records = db.command(command).execute(userId, source);
    return records == 1;
  }
}
