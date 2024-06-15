package org.sixtysix.persistence

interface Persistable {
    val _id: String
    var _rev: String?
}