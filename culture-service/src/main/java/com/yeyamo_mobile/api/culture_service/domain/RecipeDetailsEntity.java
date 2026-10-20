package com.yeyamo_mobile.api.culture_service.domain;

import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "culture_recipe_details")
public class RecipeDetailsEntity {
    @Id @Column(name = "content_id") private UUID contentId;
    @OneToOne(fetch = FetchType.LAZY) @MapsId @JoinColumn(name = "content_id") private CultureContent content;
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "display_order") private List<RecipeIngredient> ingredients = new ArrayList<>();
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "display_order") private List<RecipeStep> steps = new ArrayList<>();
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "display_order") private List<RecipeEquipment> equipment = new ArrayList<>();
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "display_order") private List<RecipeMedia> gallery = new ArrayList<>();
    @Column(name="prep_time_minutes") private Integer prepTimeMinutes;
    @Column(name="cook_time_minutes") private Integer cookTimeMinutes;
    @Column(name="rest_time_minutes") private Integer restTimeMinutes;
    private Integer servings;
    @Enumerated(EnumType.STRING) private RecipeDifficulty difficulty;
    @Column(name="yield_text",length=255) private String yieldText;
    @Column(name="hero_media_id") private UUID heroMediaId;
    @Column(name="video_media_id") private UUID videoMediaId;
    @Column(name="youtube_video_url",length=512) private String youtubeVideoUrl;
    @Column(name="preparation_ahead",columnDefinition="TEXT") private String preparationAhead;
    @Column(columnDefinition="TEXT") private String plating;
    @Column(columnDefinition="TEXT") private String storage;
    @Column(columnDefinition="TEXT") private String reheating;
    @Column(columnDefinition="TEXT") private String tips;
    @Column(columnDefinition="TEXT") private String variants;
    @Column(columnDefinition="TEXT") private String accompaniments;
    @Column(name="safety_notes",columnDefinition="TEXT") private String safetyNotes;

    protected RecipeDetailsEntity() {}
    public static RecipeDetailsEntity create(CultureContent content,List<RecipeIngredient> ingredients,List<RecipeStep> steps,Integer prep,Integer servings){
        RecipeDetailsEntity d=new RecipeDetailsEntity();d.content=content;d.prepTimeMinutes=prep;d.servings=servings;d.replaceIngredients(ingredients);d.replaceSteps(steps);return d;
    }
    public void enrich(Integer cook,Integer rest,RecipeDifficulty difficulty,String yieldText,UUID hero,UUID video,String youtube,String ahead,String plating,String storage,String reheating,String tips,String variants,String accompaniments,String safety,List<RecipeEquipment> equipment,List<RecipeMedia> gallery){
        this.cookTimeMinutes=cook;this.restTimeMinutes=rest;this.difficulty=difficulty;this.yieldText=trim(yieldText);this.heroMediaId=hero;this.videoMediaId=video;this.youtubeVideoUrl=trim(youtube);this.preparationAhead=trim(ahead);this.plating=trim(plating);this.storage=trim(storage);this.reheating=trim(reheating);this.tips=trim(tips);this.variants=trim(variants);this.accompaniments=trim(accompaniments);this.safetyNotes=trim(safety);
        this.equipment.clear();if(equipment!=null)equipment.forEach(e->{e.recipe=this;this.equipment.add(e);});this.gallery.clear();if(gallery!=null)gallery.forEach(m->{m.recipe=this;this.gallery.add(m);});
    }
    public void synchronizeFrom(RecipeDetailsEntity candidate){
        this.prepTimeMinutes=candidate.prepTimeMinutes;this.servings=candidate.servings;
        this.cookTimeMinutes=candidate.cookTimeMinutes;this.restTimeMinutes=candidate.restTimeMinutes;this.difficulty=candidate.difficulty;this.yieldText=candidate.yieldText;
        this.heroMediaId=candidate.heroMediaId;this.videoMediaId=candidate.videoMediaId;this.youtubeVideoUrl=candidate.youtubeVideoUrl;this.preparationAhead=candidate.preparationAhead;
        this.plating=candidate.plating;this.storage=candidate.storage;this.reheating=candidate.reheating;this.tips=candidate.tips;this.variants=candidate.variants;this.accompaniments=candidate.accompaniments;this.safetyNotes=candidate.safetyNotes;
        Map<UUID,RecipeIngredient> existingIngredients=new HashMap<>();ingredients.forEach(value->existingIngredients.put(value.id,value));
        List<RecipeIngredient> nextIngredients=new ArrayList<>();
        for(RecipeIngredient incoming:candidate.ingredients){RecipeIngredient current=existingIngredients.get(incoming.id);if(current==null)current=incoming;else current.copyValuesFrom(incoming);current.recipe=this;nextIngredients.add(current);}
        ingredients.clear();ingredients.addAll(nextIngredients);
        Map<UUID,RecipeIngredient> ingredientsById=new HashMap<>();ingredients.forEach(value->ingredientsById.put(value.id,value));
        Map<UUID,RecipeStep> existingSteps=new HashMap<>();steps.forEach(value->existingSteps.put(value.id,value));
        List<RecipeStep> nextSteps=new ArrayList<>();
        for(RecipeStep incoming:candidate.steps){RecipeStep current=existingSteps.get(incoming.id);if(current==null)current=incoming;else current.copyValuesFrom(incoming);current.recipe=this;current.linkIngredients(incoming.ingredients.stream().map(value->ingredientsById.get(value.id)).filter(Objects::nonNull).toList());nextSteps.add(current);}
        steps.clear();steps.addAll(nextSteps);
        synchronizeEquipment(candidate.equipment);synchronizeMedia(candidate.gallery);
    }
    private void synchronizeEquipment(List<RecipeEquipment> incoming){Map<UUID,RecipeEquipment> existing=new HashMap<>();equipment.forEach(value->existing.put(value.id,value));List<RecipeEquipment> next=new ArrayList<>();for(RecipeEquipment value:incoming){RecipeEquipment current=existing.get(value.id);if(current==null)current=value;else current.copyValuesFrom(value);current.recipe=this;next.add(current);}equipment.clear();equipment.addAll(next);}
    private void synchronizeMedia(List<RecipeMedia> incoming){Map<UUID,RecipeMedia> existing=new HashMap<>();gallery.forEach(value->existing.put(value.id,value));List<RecipeMedia> next=new ArrayList<>();for(RecipeMedia value:incoming){RecipeMedia current=existing.get(value.id);if(current==null)current=value;else current.copyValuesFrom(value);current.recipe=this;next.add(current);}gallery.clear();gallery.addAll(next);}
    private void replaceIngredients(List<RecipeIngredient> values){ingredients.clear();if(values!=null)values.forEach(v->{v.recipe=this;ingredients.add(v);});}
    private void replaceSteps(List<RecipeStep> values){steps.clear();if(values!=null)values.forEach(v->{v.recipe=this;steps.add(v);});}
    public UUID getContentId(){return contentId;} public List<RecipeIngredient> getIngredients(){return List.copyOf(ingredients);} public List<RecipeStep> getSteps(){return List.copyOf(steps);} public List<RecipeEquipment> getEquipment(){return List.copyOf(equipment);} public List<RecipeMedia> getGallery(){return List.copyOf(gallery);}
    public Integer getPrepTimeMinutes(){return prepTimeMinutes;} public Integer getCookTimeMinutes(){return cookTimeMinutes;} public Integer getRestTimeMinutes(){return restTimeMinutes;} public Integer getServings(){return servings;} public RecipeDifficulty getDifficulty(){return difficulty;} public String getYieldText(){return yieldText;} public UUID getHeroMediaId(){return heroMediaId;} public UUID getVideoMediaId(){return videoMediaId;} public String getYoutubeVideoUrl(){return youtubeVideoUrl;} public String getPreparationAhead(){return preparationAhead;} public String getPlating(){return plating;} public String getStorage(){return storage;} public String getReheating(){return reheating;} public String getTips(){return tips;} public String getVariants(){return variants;} public String getAccompaniments(){return accompaniments;} public String getSafetyNotes(){return safetyNotes;}

    @Entity @Table(name="culture_recipe_ingredients")
    public static class RecipeIngredient {
        @Id private UUID id;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="content_id",nullable=false) private RecipeDetailsEntity recipe;
        @Column(nullable=false,length=255) private String name; @Column(length=80) private String quantity; @Column(length=80) private String unit;
        @Column(name="group_name",length=120) private String groupName; @Column(nullable=false) private boolean optional;
        @Column(name="substitution_notes",columnDefinition="TEXT") private String substitutionNotes;
        @Column(name="quantity_value",precision=19,scale=6) private BigDecimal quantityValue;
        @Column(name="quantity_max_value",precision=19,scale=6) private BigDecimal quantityMaxValue;
        @Enumerated(EnumType.STRING) @Column(name="quantity_unit_code",length=16) private QuantityUnitCode quantityUnitCode;
        protected RecipeIngredient(){}
        public RecipeIngredient(String name,String quantity,String unit){this(null,name,quantity,unit,null,false,null,null,null,null);}
        public RecipeIngredient(UUID id,String name,String quantity,String unit,String groupName,boolean optional,String substitutionNotes,BigDecimal min,BigDecimal max,QuantityUnitCode code){this.id=id==null?UUID.randomUUID():id;this.name=name.trim();this.quantity=trim(quantity);this.unit=trim(unit);this.groupName=trim(groupName);this.optional=optional;this.substitutionNotes=trim(substitutionNotes);if(min!=null&&max!=null&&max.compareTo(min)<0)throw new IllegalArgumentException("quantityMaxValue must be greater than or equal to quantityValue");this.quantityValue=min;this.quantityMaxValue=max;this.quantityUnitCode=code;}
        private void copyValuesFrom(RecipeIngredient value){this.name=value.name;this.quantity=value.quantity;this.unit=value.unit;this.groupName=value.groupName;this.optional=value.optional;this.substitutionNotes=value.substitutionNotes;this.quantityValue=value.quantityValue;this.quantityMaxValue=value.quantityMaxValue;this.quantityUnitCode=value.quantityUnitCode;}
        public UUID getId(){return id;} public String getName(){return name;} public String getQuantity(){return quantity;} public String getUnit(){return unit;} public String getGroupName(){return groupName;} public boolean isOptional(){return optional;} public String getSubstitutionNotes(){return substitutionNotes;} public BigDecimal getQuantityValue(){return quantityValue;} public BigDecimal getQuantityMaxValue(){return quantityMaxValue;} public QuantityUnitCode getQuantityUnitCode(){return quantityUnitCode;}
    }

    @Entity @Table(name="culture_recipe_steps")
    public static class RecipeStep {
        @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="content_id",nullable=false) private RecipeDetailsEntity recipe;
        @Column(length=255) private String title; @Column(nullable=false,columnDefinition="TEXT") private String instruction;
        @Column(name="duration_minutes") private Integer durationMinutes; @Column(name="temperature_value",precision=8,scale=2) private BigDecimal temperatureValue;
        @Enumerated(EnumType.STRING) @Column(name="temperature_unit",length=4) private TemperatureUnit temperatureUnit;
        @Column(name="expected_result",columnDefinition="TEXT") private String expectedResult; @Column(columnDefinition="TEXT") private String tip; @Column(name="safety_note",columnDefinition="TEXT") private String safetyNote; @Column(name="media_id") private UUID mediaId;
        @ManyToMany @JoinTable(name="culture_recipe_step_ingredients",joinColumns=@JoinColumn(name="step_id"),inverseJoinColumns=@JoinColumn(name="ingredient_id")) private Set<RecipeIngredient> ingredients=new LinkedHashSet<>();
        protected RecipeStep(){} public RecipeStep(String instruction){this(null,null,instruction,null,null,null,null,null,null,null);}
        public RecipeStep(UUID id,String title,String instruction,Integer duration,BigDecimal temperature,TemperatureUnit unit,String expected,String tip,String safety,UUID mediaId){this.id=id==null?UUID.randomUUID():id;this.title=trim(title);this.instruction=instruction.trim();this.durationMinutes=duration;this.temperatureValue=temperature;this.temperatureUnit=unit;this.expectedResult=trim(expected);this.tip=trim(tip);this.safetyNote=trim(safety);this.mediaId=mediaId;}
        private void copyValuesFrom(RecipeStep value){this.title=value.title;this.instruction=value.instruction;this.durationMinutes=value.durationMinutes;this.temperatureValue=value.temperatureValue;this.temperatureUnit=value.temperatureUnit;this.expectedResult=value.expectedResult;this.tip=value.tip;this.safetyNote=value.safetyNote;this.mediaId=value.mediaId;}
        public void linkIngredients(Collection<RecipeIngredient> values){ingredients.clear();if(values!=null)ingredients.addAll(values);}
        public UUID getId(){return id;} public String getTitle(){return title;} public String getInstruction(){return instruction;} public Integer getDurationMinutes(){return durationMinutes;} public BigDecimal getTemperatureValue(){return temperatureValue;} public TemperatureUnit getTemperatureUnit(){return temperatureUnit;} public String getExpectedResult(){return expectedResult;} public String getTip(){return tip;} public String getSafetyNote(){return safetyNote;} public UUID getMediaId(){return mediaId;} public Set<RecipeIngredient> getIngredients(){return Set.copyOf(ingredients);}
    }

    @Entity @Table(name="culture_recipe_equipment") public static class RecipeEquipment { @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY)@JoinColumn(name="content_id",nullable=false)private RecipeDetailsEntity recipe;@Column(nullable=false,length=255)private String name;@Column(nullable=false)private boolean optional;protected RecipeEquipment(){}public RecipeEquipment(UUID id,String name,boolean optional){this.id=id==null?UUID.randomUUID():id;this.name=name.trim();this.optional=optional;}private void copyValuesFrom(RecipeEquipment value){name=value.name;optional=value.optional;}public UUID getId(){return id;}public String getName(){return name;}public boolean isOptional(){return optional;} }
    @Entity @Table(name="culture_recipe_media") public static class RecipeMedia { @Id private UUID id;@ManyToOne(fetch=FetchType.LAZY)@JoinColumn(name="content_id",nullable=false)private RecipeDetailsEntity recipe;@Column(name="media_id",nullable=false)private UUID mediaId;@Enumerated(EnumType.STRING)@Column(nullable=false)private RecipeMediaRole role;@Column(length=500)private String caption;@Column(name="alt_text",length=500)private String altText;protected RecipeMedia(){}public RecipeMedia(UUID id,UUID mediaId,RecipeMediaRole role,String caption,String altText){this.id=id==null?UUID.randomUUID():id;this.mediaId=Objects.requireNonNull(mediaId);this.role=Objects.requireNonNull(role);this.caption=trim(caption);this.altText=trim(altText);}private void copyValuesFrom(RecipeMedia value){mediaId=value.mediaId;role=value.role;caption=value.caption;altText=value.altText;}public UUID getId(){return id;}public UUID getMediaId(){return mediaId;}public RecipeMediaRole getRole(){return role;}public String getCaption(){return caption;}public String getAltText(){return altText;} }
    private static String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}
