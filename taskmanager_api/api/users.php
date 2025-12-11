<?php
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        // Получить данные пользователя по ID
        if (isset($_GET['user_id'])) {
            $user_id = intval($_GET['user_id']);
            
            $query = "SELECT idusers, name, email, image, coins, created_at, 
                             updated_at, last_login, is_active 
                      FROM users WHERE idusers = :user_id";
            
            $stmt = $db->prepare($query);
            $stmt->bindParam(":user_id", $user_id);
            $stmt->execute();
            
            if ($stmt->rowCount() > 0) {
                $row = $stmt->fetch(PDO::FETCH_ASSOC);
                
                $user_data = array(
                    "idusers" => $row['idusers'],
                    "name" => $row['name'],
                    "email" => $row['email'],
                    "image" => $row['image'],
                    "coins" => $row['coins'],
                    "created_at" => $row['created_at'],
                    "updated_at" => $row['updated_at'],
                    "last_login" => $row['last_login'],
                    "is_active" => $row['is_active'] == 1
                );
                
                echo json_encode(array(
                    "success" => true,
                    "user" => $user_data
                ));
            } else {
                http_response_code(404);
                echo json_encode(array(
                    "message" => "Пользователь не найден",
                    "success" => false
                ));
            }
        } 
        // Получить всех пользователей (для админки)
        else {
            $query = "SELECT idusers, name, email, image, coins, created_at, 
                             last_login, is_active 
                      FROM users WHERE is_active = 1 ORDER BY created_at DESC";
            
            $stmt = $db->prepare($query);
            $stmt->execute();
            
            $users = array();
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                $users[] = array(
                    "idusers" => $row['idusers'],
                    "name" => $row['name'],
                    "email" => $row['email'],
                    "image" => $row['image'],
                    "coins" => $row['coins'],
                    "created_at" => $row['created_at'],
                    "last_login" => $row['last_login'],
                    "is_active" => $row['is_active'] == 1
                );
            }
            
            echo json_encode(array(
                "success" => true,
                "users" => $users,
                "count" => count($users)
            ));
        }
        break;
        
    case 'PUT':
        // Обновить данные пользователя
        $data = json_decode(file_get_contents("php://input"));
        
        if (!empty($data->user_id)) {
            $user_id = intval($data->user_id);
            
            // Проверяем существование пользователя
            $check_query = "SELECT idusers FROM users WHERE idusers = :user_id";
            $check_stmt = $db->prepare($check_query);
            $check_stmt->bindParam(":user_id", $user_id);
            $check_stmt->execute();
            
            if ($check_stmt->rowCount() == 0) {
                http_response_code(404);
                echo json_encode(array(
                    "message" => "Пользователь не найден",
                    "success" => false
                ));
                exit;
            }
            
            // Строим запрос обновления на основе переданных полей
            $update_fields = array();
            $params = array(":user_id" => $user_id);
            
            if (isset($data->name)) {
                $update_fields[] = "name = :name";
                $params[":name"] = htmlspecialchars(strip_tags($data->name));
            }
            
            if (isset($data->email)) {
                // Проверяем уникальность email
                if ($data->email != $data->current_email) {
                    $email_check = "SELECT idusers FROM users WHERE email = :email AND idusers != :user_id";
                    $email_stmt = $db->prepare($email_check);
                    $email_stmt->bindParam(":email", $data->email);
                    $email_stmt->bindParam(":user_id", $user_id);
                    $email_stmt->execute();
                    
                    if ($email_stmt->rowCount() > 0) {
                        http_response_code(400);
                        echo json_encode(array(
                            "message" => "Этот email уже используется другим пользователем",
                            "success" => false
                        ));
                        exit;
                    }
                }
                
                $update_fields[] = "email = :email";
                $params[":email"] = htmlspecialchars(strip_tags($data->email));
            }
            
            if (isset($data->image)) {
                $update_fields[] = "image = :image";
                $params[":image"] = htmlspecialchars(strip_tags($data->image));
            }
            
            if (isset($data->coins)) {
                $update_fields[] = "coins = :coins";
                $params[":coins"] = intval($data->coins);
            }
            
            if (isset($data->password) && !empty($data->password)) {
                $update_fields[] = "password = :password";
                $params[":password"] = password_hash($data->password, PASSWORD_DEFAULT);
            }
            
            // Всегда обновляем updated_at
            $update_fields[] = "updated_at = NOW()";
            
            if (count($update_fields) > 0) {
                $query = "UPDATE users SET " . implode(", ", $update_fields) . 
                         " WHERE idusers = :user_id";
                
                $stmt = $db->prepare($query);
                
                // Привязываем все параметры
                foreach ($params as $key => $value) {
                    $stmt->bindValue($key, $value);
                }
                
                if ($stmt->execute()) {
                    // Получаем обновленные данные пользователя
                    $get_query = "SELECT idusers, name, email, image, coins, 
                                         created_at, updated_at, last_login, is_active 
                                  FROM users WHERE idusers = :user_id";
                    $get_stmt = $db->prepare($get_query);
                    $get_stmt->bindParam(":user_id", $user_id);
                    $get_stmt->execute();
                    
                    $row = $get_stmt->fetch(PDO::FETCH_ASSOC);
                    
                    $user_data = array(
                        "idusers" => $row['idusers'],
                        "name" => $row['name'],
                        "email" => $row['email'],
                        "image" => $row['image'],
                        "coins" => $row['coins'],
                        "created_at" => $row['created_at'],
                        "updated_at" => $row['updated_at'],
                        "last_login" => $row['last_login'],
                        "is_active" => $row['is_active'] == 1
                    );
                    
                    echo json_encode(array(
                        "message" => "Данные пользователя обновлены",
                        "success" => true,
                        "user" => $user_data
                    ));
                } else {
                    http_response_code(500);
                    echo json_encode(array(
                        "message" => "Ошибка при обновлении данных",
                        "success" => false
                    ));
                }
            } else {
                http_response_code(400);
                echo json_encode(array(
                    "message" => "Нет данных для обновления",
                    "success" => false
                ));
            }
        } else {
            http_response_code(400);
            echo json_encode(array(
                "message" => "Не указан ID пользователя",
                "success" => false
            ));
        }
        break;
        
    case 'DELETE':
        // Удалить пользователя (деактивировать)
        $data = json_decode(file_get_contents("php://input"));
        
        if (!empty($data->user_id)) {
            $user_id = intval($data->user_id);
            
            $query = "UPDATE users SET is_active = 0, updated_at = NOW() 
                      WHERE idusers = :user_id";
            
            $stmt = $db->prepare($query);
            $stmt->bindParam(":user_id", $user_id);
            
            if ($stmt->execute()) {
                echo json_encode(array(
                    "message" => "Пользователь деактивирован",
                    "success" => true
                ));
            } else {
                http_response_code(500);
                echo json_encode(array(
                    "message" => "Ошибка при деактивации пользователя",
                    "success" => false
                ));
            }
        } else {
            http_response_code(400);
            echo json_encode(array(
                "message" => "Не указан ID пользователя",
                "success" => false
            ));
        }
        break;
        
    case 'POST':
        // Обновление coins пользователя
        $data = json_decode(file_get_contents("php://input"));
        
        if (!empty($data->user_id) && isset($data->coins)) {
            $user_id = intval($data->user_id);
            $coins = intval($data->coins);
            
            $query = "UPDATE users SET coins = :coins, updated_at = NOW() 
                      WHERE idusers = :user_id";
            
            $stmt = $db->prepare($query);
            $stmt->bindParam(":user_id", $user_id);
            $stmt->bindParam(":coins", $coins);
            
            if ($stmt->execute()) {
                // Получаем обновленные coins
                $get_query = "SELECT coins FROM users WHERE idusers = :user_id";
                $get_stmt = $db->prepare($get_query);
                $get_stmt->bindParam(":user_id", $user_id);
                $get_stmt->execute();
                
                $row = $get_stmt->fetch(PDO::FETCH_ASSOC);
                
                echo json_encode(array(
                    "message" => "Coins обновлены",
                    "success" => true,
                    "coins" => $row['coins']
                ));
            } else {
                http_response_code(500);
                echo json_encode(array(
                    "message" => "Ошибка при обновлении coins",
                    "success" => false
                ));
            }
        } else {
            http_response_code(400);
            echo json_encode(array(
                "message" => "Не указаны user_id или coins",
                "success" => false
            ));
        }
        break;
        
    default:
        http_response_code(405);
        echo json_encode(array(
            "message" => "Метод не поддерживается",
            "success" => false
        ));
        break;
}
?>