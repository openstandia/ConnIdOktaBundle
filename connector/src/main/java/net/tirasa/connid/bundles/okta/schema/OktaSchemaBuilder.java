/**
 * Copyright © 2019 ConnId (connid-dev@googlegroups.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.connid.bundles.okta.schema;


import com.okta.sdk.client.Client;
import com.okta.sdk.resource.ExtensibleResource;
import net.tirasa.connid.bundles.okta.OktaConnector;
import net.tirasa.connid.bundles.okta.utils.OktaAttribute;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.spi.operations.SearchOp;

import java.util.*;

class OktaSchemaBuilder {

    private static final Log LOG = Log.getLog(OktaSchemaBuilder.class);

    public static final String SCHEMA_DEFINITIONS = "definitions";

    public static final String SCHEMA_BASE = "base";

    public static final String SCHEMA_CUSTOM = "custom";

    public static final String TYPE = "type";

    public static final String REQUIRED = "required";

    private static final String PROPERTIES = "properties";

    public static final List<String> ATTRS_TYPE = new ArrayList<String>() {

        private static final long serialVersionUID = 5636572627689425575L;

        {
            add(SCHEMA_BASE);
            add(SCHEMA_CUSTOM);
        }
    };

    private final Client client;

    private Schema schema;

    public OktaSchemaBuilder(final Client client) {
        this.client = client;
    }

    public Schema getSchema() {
        if (schema == null) {
            buildSchema();
        }
        return schema;
    }

    private void buildSchema() {
        final SchemaBuilder schemaBld = new SchemaBuilder(OktaConnector.class);
        buildAccount(ObjectClass.ACCOUNT_NAME, schemaBld);
        buildGroup(ObjectClass.GROUP_NAME, schemaBld);
        buildApplication(OktaConnector.APPLICATION_NAME, schemaBld);
//        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildAttributesToGet(), SearchOp.class);
//        schemaBld.defineOperationOption(OperationOptionInfoBuilder.buildReturnDefaultAttributes(), SearchOp.class);
        schema = schemaBld.build();
    }

    private ObjectClassInfoBuilder build(final String objectClassName) {
        final ObjectClassInfoBuilder objClassBld = new ObjectClassInfoBuilder();
        objClassBld.setType(objectClassName);
        objClassBld.setContainer(false);
        return objClassBld;
    }

    private void buildAccount(final String objectClassName, final SchemaBuilder schemaBld) {
        ObjectClassInfo oci = build(objectClassName).addAllAttributeInfo(buildAccountAttrInfos()).build();
        schemaBld.defineObjectClass(oci);
    }

    private void buildGroup(final String objectClassName, final SchemaBuilder schemaBld) {
        ObjectClassInfo oci = build(objectClassName).addAllAttributeInfo(buildGroupAttrInfos()).build();
        schemaBld.defineObjectClass(oci);
    }

    private void buildApplication(final String objectClassName, final SchemaBuilder schemaBld) {
        ObjectClassInfo oci = build(objectClassName).addAllAttributeInfo(buildApplicationAttrInfos()).build();
        schemaBld.defineObjectClass(oci);
    }

    @SuppressWarnings({"unchecked"})
    private Collection<AttributeInfo> buildAccountAttrInfos() {
        LOG.ok("Retrieve User schema profile");
        final List<AttributeInfo> attributeInfos = new ArrayList<>();
        ExtensibleResource userSchema =
                client.getDataStore().http().get(OktaConnector.SCHEMA_USER_EDITOR_PROFILE_API_URL, ExtensibleResource.class);
        Map<String, Object> definitions = Map.class.cast(userSchema.get(SCHEMA_DEFINITIONS));
        ATTRS_TYPE.stream().forEach(item -> {
            List<String> requiredAttrs = ((Map<String, List<String>>) definitions.get(item)).get(REQUIRED);
            Map<String, Object> schemas = (Map<String, Object>) definitions.get(item);
            ((Map<String, Object>) schemas.get(PROPERTIES)).entrySet().stream().filter(p -> !p.getKey().equals(OktaAttribute.LOGIN)).forEach(
                    schemaDef -> {
                        final Set<AttributeInfo.Flags> flags = EnumSet.noneOf(Flags.class);
                        final AttributeInfoBuilder attributeInfo = new AttributeInfoBuilder();
                        attributeInfo.setRequired(requiredAttrs != null && requiredAttrs.contains(schemaDef.getKey()));
                        attributeInfos.add(AttributeInfoBuilder.build(schemaDef.getKey(),
                                OktaAttribute.getType(((Map<String, String>) schemaDef.getValue()).get(TYPE))));
                    });
        });

        attributeInfos.add(AttributeInfoBuilder.define(Uid.NAME)
                .setRequired(false) // Must be optional. It is not present for create operations
                .setCreateable(false)
                .setUpdateable(false)
                .setNativeName(OktaAttribute.USER_ID)
                .build());

        attributeInfos.add(AttributeInfoBuilder.define(Name.NAME)
                .setRequired(true) // Must be optional. It is not present for create operations
                .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                .setNativeName(OktaAttribute.LOGIN)
                .build());

        attributeInfos.add(AttributeInfoBuilder.define(OktaAttribute.OKTA_GROUPS, String.class)
                .setMultiValued(true)
                .setReturnedByDefault(false)
                .build());

        return attributeInfos;
    }

    private Collection<AttributeInfo> buildGroupAttrInfos() {
        LOG.ok("Retrieve Group schema profile");
        final List<AttributeInfo> attributeInfos = new ArrayList<>();
        attributeInfos.add(AttributeInfoBuilder.build(OktaAttribute.DESCRIPTION, String.class));

        attributeInfos.add(AttributeInfoBuilder.define(Uid.NAME)
                .setRequired(false) // Must be optional. It is not present for create operations
                .setCreateable(false)
                .setUpdateable(false)
                .setNativeName(OktaAttribute.GROUP_ID)
                .build());

        attributeInfos.add(AttributeInfoBuilder.define(Name.NAME)
                .setRequired(true) // Must be optional. It is not present for create operations
                .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                .setNativeName(OktaAttribute.NAME)
                .build());

        return attributeInfos;
    }

    private Collection<AttributeInfo> buildApplicationAttrInfos() {
        LOG.ok("Retrieve Application schema profile");
        final List<AttributeInfo> attributeInfos = new ArrayList<>();
        attributeInfos.add(AttributeInfoBuilder.build(OktaAttribute.LABEL, String.class));

        attributeInfos.add(AttributeInfoBuilder.define(Uid.NAME)
                .setRequired(false) // Must be optional. It is not present for create operations
                .setCreateable(false)
                .setUpdateable(false)
                .setNativeName(OktaAttribute.APPLICATION_ID)
                .build());

        attributeInfos.add(AttributeInfoBuilder.define(Name.NAME)
                .setRequired(true) // Must be optional. It is not present for create operations
                .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                .setNativeName(OktaAttribute.NAME)
                .build());

        return attributeInfos;
    }
}
