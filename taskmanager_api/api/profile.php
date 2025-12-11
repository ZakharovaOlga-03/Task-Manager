<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($method !== 'GET') {
    http_response_code(405);
    echo json_encode(array("message" => "Метод не поддерживается", "success" => false));
    exit;
}

if (!isset($_GET['user_id'])) {
    http_response_code(400);
    echo json_encode(array("message" => "Не указан user_id", "success" => false));
    exit;
}

$user_id = intval($_GET['user_id']);

try {
    $query = "SELECT 
                id_user, 
                name, 
                email, 
                account_type,
                created_at,
                premium_until
              FROM users 
              WHERE id_user = :user_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        
        $user_data = array(
            "id_user" => (int)$row['id_user'],
            "name" => $row['name'],
            "email" => $row['email'],
            "account_type" => $row['account_type'] ?: "basic",
            "created_at" => $row['created_at'],
            "premium_until" => $row['premium_until']
        );
        
        echo json_encode(array(
            "success" => true,
            "message" => "Профиль загружен",
            "user" => $user_data
        ), JSON_UNESCAPED_UNICODE);
    } else {
        http_response_code(404);
        echo json_encode(array(
            "success" => false,
            "message" => "Пользователь не найден"
        ));
    }
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(array(
        "success" => false,
        "message" => "Ошибка базы данных: " . $e->getMessage()
    ));
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(array(
        "success" => false,
        "message" => "Ошибка: " . $e->getMessage()
    ));
}
?>