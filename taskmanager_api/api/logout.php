<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($method !== 'POST') {
    http_response_code(405);
    echo json_encode(array("message" => "Метод не поддерживается", "success" => false));
    exit;
}

$data = json_decode(file_get_contents("php://input"));

if (!isset($data->user_id)) {
    http_response_code(400);
    echo json_encode(array("message" => "Не указан user_id", "success" => false));
    exit;
}

// Здесь можно добавить логику для инвалидации токенов,
// очистки сессий и т.д. если используете JWT или сессии

echo json_encode(array(
    "success" => true,
    "message" => "Успешный выход"
));
?>