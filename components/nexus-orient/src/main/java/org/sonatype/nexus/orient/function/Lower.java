package org.sonatype.nexus.orient.function;

import javax.inject.Named;
import javax.inject.Singleton;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @since 3.0
 */
@Named
@Singleton
public class Lower
    extends OSQLFunctionAbstract
{
  public Lower() {
    super("lower", 1, 1);
  }

  @Override
  public Object execute(final Object iThis, final OIdentifiable iCurrentRecord, final Object iCurrentResult,
                        final Object[] iParams,
                        final OCommandContext iContext)
  {
    checkArgument(iParams.length == 1);

    final Object param = iParams[0];

    if (param == null) {
      return null;
    }

    checkArgument(param instanceof String, "lower() parameter must be a string");

    return ((String) param).toLowerCase();
  }


  @Override
  public String getSyntax() {
    return "lower(<string>)";
  }

  @Override
  public boolean aggregateResults() {
    return false;
  }
}
