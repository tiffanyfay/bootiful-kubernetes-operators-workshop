/*
 * Kubernetes
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1.21.1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


 package io.spring.controller.models;

 import java.util.Objects;
 import java.util.Arrays;
 import com.google.gson.TypeAdapter;
 import com.google.gson.annotations.JsonAdapter;
 import com.google.gson.annotations.SerializedName;
 import com.google.gson.stream.JsonReader;
 import com.google.gson.stream.JsonWriter;
 import io.swagger.annotations.ApiModel;
 import io.swagger.annotations.ApiModelProperty;
 import java.io.IOException;
 
 /**
  * V1FooSpec
  */
 @javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-08-21T19:16:14.455Z[Etc/UTC]")
 public class V1FooSpec {
   public static final String SERIALIZED_NAME_NICKNAME = "nickname";
   @SerializedName(SERIALIZED_NAME_NICKNAME)
   private String nickname;
 
 
   public V1FooSpec nickname(String nickname) {
     
     this.nickname = nickname;
     return this;
   }
 
    /**
    * The nickname of your Foo
    * @return nickname
   **/
   @javax.annotation.Nullable
   @ApiModelProperty(value = "The nickname of your Foo")
 
   public String getNickname() {
     return nickname;
   }
 
 
   public void setNickname(String nickname) {
     this.nickname = nickname;
   }
 
 
   @Override
   public boolean equals(Object o) {
     if (this == o) {
       return true;
     }
     if (o == null || getClass() != o.getClass()) {
       return false;
     }
     V1FooSpec v1FooSpec = (V1FooSpec) o;
     return Objects.equals(this.nickname, v1FooSpec.nickname);
   }
 
   @Override
   public int hashCode() {
     return Objects.hash(nickname);
   }
 
 
   @Override
   public String toString() {
     StringBuilder sb = new StringBuilder();
     sb.append("class V1FooSpec {\n");
     sb.append("    nickname: ").append(toIndentedString(nickname)).append("\n");
     sb.append("}");
     return sb.toString();
   }
 
   /**
    * Convert the given object to string with each line indented by 4 spaces
    * (except the first line).
    */
   private String toIndentedString(Object o) {
     if (o == null) {
       return "null";
     }
     return o.toString().replace("\n", "\n    ");
   }
 
 }
 
 