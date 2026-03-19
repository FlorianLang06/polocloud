package de.polocloud.node.services.templates

class TemplateRegistry {

    private val templates = mutableMapOf<String, ServiceTemplate>()

    fun register(template: ServiceTemplate) {
        templates[template.name] = template
    }

    fun registerAll(list: List<ServiceTemplate>) {
        list.forEach { register(it) }
    }


    fun get(name: String): ServiceTemplate? = templates[name]

    fun getAll(): Collection<ServiceTemplate> = templates.values
}