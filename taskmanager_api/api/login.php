<?php
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->email) && !empty($data->password)) {
    $query = "SELECT idusers, name, email, password, coins, image FROM users 
              WHERE email = :email AND is_active = 1";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":email", $data->email);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (password_verify($data->password, $row['password'])) {
            // Обновляем last_login
            $update_query = "UPDATE users SET last_login = NOW() WHERE idusers = :id";
            $update_stmt = $db->prepare($update_query);
            $update_stmt->bindParam(":id", $row['idusers']);
            $update_stmt->execute();
            
            // Возвращаем данные пользователя (без пароля)
            $user_data = array(
                "user_id" => $row['idusers'],
                "name" => $row['name'],
                "email" => $row['email'],
                "coins" => $row['coins'],
                "image" => $row['image']
            );
            
            echo json_encode(array(
                "message" => "Авторизация успешна",
                "success" => true,
                "user" => $user_data
            ));
        } else {
            http_response_code(401);
            echo json_encode(array("message" => "Неверный пароль", "success" => false));
        }
    } else {
        http_response_code(404);
        echo json_encode(array("message" => "Пользователь не найден", "success" => false));
    }
} else {
    http_response_code(400);
    echo json_encode(array("message" => "Неполные данные", "success" => false));
}
?>