plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.9"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "26.3") {
            // Constructor was replaced with a method call
            replace("new BundleContents.Mutable(contents);", "contents.asMutable();")
            // Method name changed
            replace("this.contents.itemCopyStream()", "this.contents.itemCopies()")
        }
        string(current.parsed >= "26.1") {
            // Method name changed
            replace("BundleItem.getSelectedItem(", "BundleItem.getSelectedItemIndex(")
            // Client-side class names changed
            replace("ClickType", "ContainerInput")
            replace("GuiGraphics", "GuiGraphicsExtractor")
        }
        string(current.parsed >= "1.21.11") {
            // Class name changed
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed >= "1.21.6") {
            // Method call returning a field was replaced with the now exposed field itself
            replace("RenderType::guiTextured", "RenderPipelines.GUI_TEXTURED")
        }
        string(current.parsed >= "1.21.5") {
            // Return type changed from long to Optional<Long>
            replace("getLong(\"randomHash\")", "getLong(\"randomHash\").orElse(0L)")
        }
    }
}
