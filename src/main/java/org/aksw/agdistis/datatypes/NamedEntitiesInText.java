package org.aksw.agdistis.datatypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NamedEntitiesInText implements Iterable<NamedEntityInText> {

  private List<NamedEntityInText> namedEntities = new ArrayList<NamedEntityInText>();

  public NamedEntitiesInText() {
  }

  public NamedEntitiesInText(final NamedEntityInText... namedEntities) {
    this(Arrays.asList(namedEntities));
  }

  public NamedEntitiesInText(final List<NamedEntityInText> namedEntities) {
    this.namedEntities.addAll(namedEntities);
  }

  public List<NamedEntityInText> getNamedEntities() {
    return namedEntities;
  }

  public void setNamedEntities(final List<NamedEntityInText> namedEntities) {
    this.namedEntities = namedEntities;
  }

  public void addNamedEntity(final NamedEntityInText namedEntity) {
    namedEntities.add(namedEntity);
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append("NamedEntitiesInText=\"[");
    boolean first = true;
    for (final NamedEntityInText ne : namedEntities) {
      if (first) {
        first = false;
      } else {
        result.append(", ");
      }
      result.append(ne.toString());
    }
    result.append(']');
    return result.toString();
  }

  @Override
  public Iterator<NamedEntityInText> iterator() {
    return namedEntities.iterator();
  }
}