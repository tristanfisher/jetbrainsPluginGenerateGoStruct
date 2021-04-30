import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Simple script for generating go structs from JetBrains database tools
 * Connect your data source, expand your schema, right click your column
 * Scripted Extensions -> whatever you name this in your script directory.
 *
 * path to save is extensions/com.intellij.database/
 *
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

// logic just lifted from 'Generate POJOS.groovy'
//
// i did not read the documentation. i also don't know how to write groovy
// https://blog.jetbrains.com/datagrip/2018/02/13/generate-pojos/
packageName = "yourPackageNameHere"
typeMapping = [
    (~/(?i)int/)                      : "int",
    (~/(?i)float|double|decimal|real/): "float64",
    (~/(?i)datetime|timestamp/)       : "time.Time",
    (~/(?i)date/)                     : "time.Time",
    (~/(?i)time/)                     : "time.Time",
    (~/(?i)/)                         : "string"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    new File(dir, className + ".go").withPrintWriter { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
    out.println "package $packageName"
    out.println ""
    out.println "type $className struct {"

    // write the struct
    fields.each() {
    // i don't know what this does:
    if (it.annos != "") out.println "  ${it.annos}"
        out.println "	${it.name}\t${it.type}"
    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
            name : javaName(col.getName(), false),
            type : typeStr,
            annos: ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
        .collect { Case.LOWER.apply(it).capitalize() }
        .join("")
        .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

