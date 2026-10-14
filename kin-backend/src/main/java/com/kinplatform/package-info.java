@org.hibernate.annotations.FilterDef(
    name = "tenantUserFilter",
    parameters = @org.hibernate.annotations.ParamDef(name = "userId", type = UUID.class)
)
package com.kinplatform;

import java.util.UUID;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;