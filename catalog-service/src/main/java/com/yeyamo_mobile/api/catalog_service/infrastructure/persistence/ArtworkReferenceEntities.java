package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;
import jakarta.persistence.*;import java.util.*;
public final class ArtworkReferenceEntities{private ArtworkReferenceEntities(){}
 @Entity@Table(name="materials")public static class Material{@Id public UUID id;@Column(unique=true)public String code;public boolean active=true;}
 @Entity@Table(name="material_translations")@IdClass(MaterialTranslationId.class)public static class MaterialTranslation{@Id@Column(name="material_id")public UUID materialId;@Id@Column(name="language_code")public String languageCode;public String name;public String description;}
 public static class MaterialTranslationId implements java.io.Serializable{public UUID materialId;public String languageCode;public MaterialTranslationId(){}public MaterialTranslationId(UUID i,String l){materialId=i;languageCode=l;}}
 @Entity@Table(name="techniques")public static class Technique{@Id public UUID id;@Column(unique=true)public String code;public boolean active=true;}
 @Entity@Table(name="technique_translations")@IdClass(TechniqueTranslationId.class)public static class TechniqueTranslation{@Id@Column(name="technique_id")public UUID techniqueId;@Id@Column(name="language_code")public String languageCode;public String name;public String description;}
 public static class TechniqueTranslationId implements java.io.Serializable{public UUID techniqueId;public String languageCode;public TechniqueTranslationId(){}public TechniqueTranslationId(UUID i,String l){techniqueId=i;languageCode=l;}}
}
