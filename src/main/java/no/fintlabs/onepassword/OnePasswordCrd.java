package no.fintlabs.onepassword;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("onepassword.com")
@Version("v1")
@Kind("OnePasswordItem")
public class OnePasswordCrd extends CustomResource<OnePasswordSpec, Void> {
}
