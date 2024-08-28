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
package com.ericsson.amonboardingservice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DockerImage {
    private String name;
    private String tag;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }

        DockerImage that = (DockerImage) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return tag != null ? tag.equals(that.tag) : that.tag == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DockerImage{" +
                "name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }

    public static List<DockerImage> getDockerImagesFromVnfPkgInfo(VnfPkgInfo vnfPkgInfo) {
        return vnfPkgInfo.getSoftwareImages().stream().
                map(image -> new DockerImage(image.getName(), image.getVersion())).collect(Collectors.toList());
    }

    public static List<DockerImage> getCommonImages(List<DockerImage> packageOneImages, List<DockerImage> packageTwoImages) {
        List<DockerImage> commonImages = new ArrayList<>(packageOneImages);
        commonImages.retainAll(packageTwoImages);
        return commonImages;
    }

    public static List<DockerImage> removeCommonImages(List<DockerImage> packageImages, List<DockerImage> commonImages) {
        packageImages.removeAll(commonImages);
        return packageImages;
    }
}
