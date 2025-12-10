<?php
require_once '../config/database.php';

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        // Получить задачи пользователя
        if (isset($_GET['user_id'])) {
            $user_id = $_GET['user_id'];
            
            $query = "SELECT * FROM tasks WHERE user_id = :user_id ORDER BY task_goal_date ASC";
            $stmt = $db->prepare($query);
            $stmt->bindParam(":user_id", $user_id);
            $stmt->execute();
            
            $tasks = array();
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                $tasks[] = array(
                    "id_task" => $row['id_task'],
                    "user_id" => $row['user_id'],
                    "task_name" => $row['task_name'],
                    "task_type" => $row['task_type'],
                    "task_importance" => $row['task_importance'],
                    "task_goal_date" => $row['task_goal_date'],
                    "notify_start" => $row['notify_start'],
                    "notify_frequency" => $row['notify_frequency'],
                    "notify_type" => $row['notify_type'],
                    "task_note" => $row['task_note'],
                    "task_reward" => $row['task_reward'],
                    "task_creation_date" => $row['task_creation_date']
                );
            }
            
            echo json_encode(array("success" => true, "tasks" => $tasks));
        } else {
            http_response_code(400);
            echo json_encode(array("message" => "Не указан user_id", "success" => false));
        }
        break;
        
    case 'POST':
        // Создать задачу
        $data = json_decode(file_get_contents("php://input"));
        
        if (!empty($data->user_id) && !empty($data->task_name)) {
            $query = "INSERT INTO tasks SET 
                      user_id = :user_id,
                      task_name = :task_name,
                      task_type = :task_type,
                      task_importance = :task_importance,
                      task_goal_date = :task_goal_date,
                      notify_start = :notify_start,
                      notify_frequency = :notify_frequency,
                      notify_type = :notify_type,
                      task_note = :task_note,
                      task_reward = :task_reward,
                      task_creation_date = NOW()";
            
            $stmt = $db->prepare($query);
            
            // Очистка данных
            $user_id = htmlspecialchars(strip_tags($data->user_id));
            $task_name = htmlspecialchars(strip_tags($data->task_name));
            $task_type = isset($data->task_type) ? htmlspecialchars(strip_tags($data->task_type)) : 'book';
            $task_importance = isset($data->task_importance) ? htmlspecialchars(strip_tags($data->task_importance)) : '3';
            $task_goal_date = isset($data->task_goal_date) ? htmlspecialchars(strip_tags($data->task_goal_date)) : date('Y-m-d H:i:s');
            $notify_start = isset($data->notify_start) ? htmlspecialchars(strip_tags($data->notify_start)) : date('Y-m-d H:i:s');
            $notify_frequency = isset($data->notify_frequency) ? htmlspecialchars(strip_tags($data->notify_frequency)) : NULL;
            $notify_type = isset($data->notify_type) ? htmlspecialchars(strip_tags($data->notify_type)) : NULL;
            $task_note = isset($data->task_note) ? htmlspecialchars(strip_tags($data->task_note)) : NULL;
            $task_reward = isset($data->task_reward) ? intval($data->task_reward) : 0;
            
            // Привязка параметров
            $stmt->bindParam(":user_id", $user_id);
            $stmt->bindParam(":task_name", $task_name);
            $stmt->bindParam(":task_type", $task_type);
            $stmt->bindParam(":task_importance", $task_importance);
            $stmt->bindParam(":task_goal_date", $task_goal_date);
            $stmt->bindParam(":notify_start", $notify_start);
            $stmt->bindParam(":notify_frequency", $notify_frequency);
            $stmt->bindParam(":notify_type", $notify_type);
            $stmt->bindParam(":task_note", $task_note);
            $stmt->bindParam(":task_reward", $task_reward, PDO::PARAM_INT);
            
            if ($stmt->execute()) {
                $task_id = $db->lastInsertId();
                echo json_encode(array(
                    "message" => "Задача создана успешно",
                    "success" => true,
                    "task_id" => $task_id
                ));
            } else {
                http_response_code(503);
                echo json_encode(array("message" => "Ошибка при создании задачи", "success" => false));
            }
        } else {
            http_response_code(400);
            echo json_encode(array("message" => "Неполные данные", "success" => false));
        }
        break;
        
    default:
        http_response_code(405);
        echo json_encode(array("message" => "Метод не поддерживается", "success" => false));
        break;
}
?>