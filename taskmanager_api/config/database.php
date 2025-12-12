<?php
class Database {
    private $host = "localhost";
    private $db_name = "taskmanager"; // ваша база данных
    private $username = "root";       // по умолчанию для XAMPP
    private $password = "";           // по умолчанию для XAMPP пустой пароль
    public $conn;

    public function getConnection() {
        $this->conn = null;
        try {
            $this->conn = new PDO(
                "mysql:host=" . $this->host . ";dbname=" . $this->db_name . ";charset=utf8mb4",
                $this->username,
                $this->password
            );
            
            // Включите режим ошибок
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->conn->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
            
            error_log("Database connection successful to $this->db_name");
        } catch(PDOException $exception) {
            error_log("Connection error: " . $exception->getMessage());
            error_log("Tried to connect to: host=$this->host, db=$this->db_name, user=$this->username");
            throw $exception;
        }
        return $this->conn;
    }
}