package dev.httpmarco.polocloud.node.database.credentials

class SqlDatabaseCredentials(val driver : String, hostname: String, port: Int, username: String, password: String, val database: String) : DatabaseCredentials(hostname, port, username, password)  {
}