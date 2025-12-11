<?php
// В начале файла добавляем заголовки CORS
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->name) && !empty($data->email) && !empty($data->password)) {
    // Проверка существования email
    $query = "SELECT idusers FROM users WHERE email = :email";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":email", $data->email);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        http_response_code(400);
        echo json_encode(["message" => "Пользователь с таким email уже существует", "success" => false]);
        exit;
    }
    
    // Создание пользователя
    $query = "INSERT INTO users SET 
              name = :name,
              email = :email,
              password = :password,
              coins = 0,
              created_at = NOW(),
              is_active = 1";
    
    $stmt = $db->prepare($query);
    
    // Очистка данных
    $name = htmlspecialchars(strip_tags($data->name));
    $email = htmlspecialchars(strip_tags($data->email));
    $password = password_hash($data->password, PASSWORD_DEFAULT);
    
    // Привязка параметров
    $stmt->bindParam(":name", $name);
    $stmt->bindParam(":email", $email);
    $stmt->bindParam(":password", $password);
    
    if ($stmt->execute()) {
        $user_id = $db->lastInsertId();
        
        http_response_code(201);
        echo json_encode([
            "message" => "Пользователь успешно зарегистрирован",
            "success" => true,
            "user_id" => $user_id,
            "name" => $name,
            "email" => $email,
            "coins" => 0
        ]);
    } else {
        http_response_code(503);
        echo json_encode(["message" => "Ошибка при регистрации", "success" => false]);
    }
} else {
    http_response_code(400);
    echo json_encode(["message" => "Неполные данные", "success" => false]);
}
?>