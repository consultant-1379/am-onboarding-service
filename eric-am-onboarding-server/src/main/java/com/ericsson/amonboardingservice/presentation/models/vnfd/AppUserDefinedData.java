/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amonboardingservice.presentation.models.vnfd;

import com.ericsson.amonboardingservice.utils.logging.ExcludeFieldsFromToStringGenerator;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "app_user_defined_data")
public class AppUserDefinedData implements Serializable {
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private static final long serialVersionUID = 346784767003818422L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private AppPackage appPackages;

    @Column(name = "user_key")
    private String key;

    @Column(name = "user_value")
    private String value;

    @Override
    public String toString() {
        ToStringStyle customStringStyle = ExcludeFieldsFromToStringGenerator.INSTANCE.getStyle(this.getClass());
        return ToStringBuilder.reflectionToString(this, customStringStyle);
    }
}
