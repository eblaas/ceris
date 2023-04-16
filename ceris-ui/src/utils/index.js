// utils/index.js

const typeMap = {
    "STRING": "string",
    "INT": "number",
    "LONG": "number",
    "SHORT": "number",
    "DOUBLE": "number",
    "BOOLEAN": "boolean",
    "LIST": "string",
    "CLASS": "string",
    "PASSWORD": "string",
}

export function generateSchema(configuration, response, hasTopic = false) {

    const configs = response.configs.filter(d => d.value.visible)

    const propertiesImportant = configs.filter(d =>
        !(d.definition.name.startsWith("transforms.") || d.definition.name.startsWith("predicates."))
        && (d.definition.importance == "HIGH" || (d.definition.required && !d.definition.default_value)))
    const schemaImportant = mapToSchema(propertiesImportant)
    const importantSection = {
        "hasError": hasErrors(propertiesImportant),
        "title": "Important",
        "type": "object",
        "properties": schemaImportant,
        "required": [...requiredNames(propertiesImportant), hasTopic ? "" : "topics"]
    }

    const commonGroups = ['Common', 'Error Handling', 'Topic Creation', 'Exactly Once Support', 'offsets.topic', '']
    const commonProps = configs.filter(d => commonGroups.includes(d.definition.group) && !(d.definition.name in schemaImportant))
    const commonSection = {
        "hasError": hasErrors(commonProps),
        "title": "Common",
        "type": "object",
        "properties": mapToSchema(commonProps),
        "required": requiredNames(commonProps)
    }

    let groupSections = response.groups.filter(g => !commonGroups.includes(g)).map(g => {
        let groupProps = configs.filter(d => d.definition.group == g && !(d.definition.name in schemaImportant))
        if (groupProps.length == 0)
            return null
        else
            return {
                "hasError": hasErrors(groupProps),
                "title": g,
                "type": "object",
                "properties": mapToSchema(groupProps),
                "required": requiredNames(groupProps)
            }
    }).filter((s) => s != null)

    const transformGroup = groupSections.find(gs => gs.properties["transforms"])
    const transformSubGroups = groupSections.filter(gs => Object.keys(gs.properties).some(p => p.startsWith("transforms.")))
    transformGroup['x-display'] = "tabs"
    transformGroup['allOf'] = transformSubGroups

    const predicateGroup = groupSections.find(gs => gs.properties["predicates"])
    const predicateSubGroups = groupSections.filter(gs => Object.keys(gs.properties).some(p => p.startsWith("predicates.")))
    predicateGroup['x-display'] = "tabs"
    predicateGroup['allOf'] = predicateSubGroups

    groupSections = groupSections.filter(gs => gs !== transformGroup && gs !== predicateGroup && !transformSubGroups.includes(gs) && !predicateSubGroups.includes(gs))

    const sections = [
        importantSection,
        ...groupSections,
        transformGroup,
        predicateGroup,
        commonSection
    ]
    return {
        valid: sections.every(s => !s.hasError),
        type: 'object',
        "x-display": "tabs",
        "allOf": sections,
    }
}

function requiredNames(properties) {
    return properties.filter(p => p.value.errors.length || p.definition.required).map(p => p.definition.name)
}

function hasErrors(properties) {
    return properties.some(p => p.value.errors.length)
}

function mapToSchema(properties) {

    let prop = {}
    properties.forEach((d) => {
        try {

            if (d.definition.display_name == "Table Loading Mode") {
                d.definition.required = true
                d.value.recommended_values = ["bulk", "incrementing", "timestamp", "timestamp+incrementing"]
            }

            prop[d.definition.name] = {
                "type": typeMap[d.definition.type],
                "title": (d.definition.display_name || d.definition.name) + ` -  (${d.definition.name})`,
                "description": d.definition.documentation?.replaceAll("*", "<br/>") + `<br/>DEFAULT: ${d.definition.default_value || ''}`
            }

            if (d.definition.default_value
                && d.definition.importance === "HIGH"
                && !(d.definition.type === "BOOLEAN" && d.definition.default_value === "false")
                && !(d.definition.default_value === "[hidden]")) {
                prop[d.definition.name]["default"] = d.definition.default_value
            }

            if (d.definition.name == "confluent.topic.bootstrap.servers") {
                prop[d.definition.name]["default"] = "${env:CERIS_SECRET_KAFKA_BOOTSTRAP_SERVERS}"
            }

            if (d.definition.name == "connector.class") {
                prop[d.definition.name]["x-display"] = "hidden"
            }

            if (d.value.recommended_values.length > 0 && d.definition.type != "BOOLEAN") {
                prop[d.definition.name]["enum"] = d.value.recommended_values
            }

            if (d.value.errors.length > 0) {
                prop[d.definition.name]["error"] = d.value.errors.join(",")
            }
            if (d.definition.type == "LIST") {
                prop[d.definition.name]["separator"] = ","
                if (d.definition.name == "topics") {
                    prop[d.definition.name]["anyOf"] = []
                }

            } else if (d.definition.type == "PASSWORD") {
                prop[d.definition.name]["x-display"] = "text"
            }
        } catch (e) {
            console.log("Failed to create schema", e)
        }
    })

    return prop
}
