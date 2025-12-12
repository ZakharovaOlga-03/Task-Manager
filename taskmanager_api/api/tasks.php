<?php

// Включите максимальный вывод ошибок
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Укажите путь для логов
ini_set('log_errors', 1);
ini_set('error_log', 'C:/xampp/htdocs/taskmanager_api/php_errors.log');

error_log("=== TASKS.PHP START ===");
error_log("Time: " . date('Y-m-d H:i:s'));
error_log("GET: " . print_r($_GET, true));

require_once '../config/database.php';

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Включите логирование для отладки
error_log("=== TASKS API CALL ===");
error_log("Method: " . $_SERVER['REQUEST_METHOD']);
error_log("Time: " . date('Y-m-d H:i:s'));
error_log("GET params: " . print_r($_GET, true));
error_log("Request body: " . file_get_contents("php://input"));

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        // Получить задачи пользователя
        if (isset($_GET['user_id'])) {
            $user_id = (int)$_GET['user_id'];
            error_log("Getting tasks for user_id: " . $user_id);
            
            // ВАЖНО: Добавьте поля task_status, is_completed, completed_date в SELECT
            $query = "SELECT 
                id_task, 
                user_id, 
                task_name, 
                task_type, 
                task_importance, 
                task_goal_date, 
                notify_start, 
                notify_frequency, 
                notify_type, 
                task_note, 
                task_reward, 
                task_creation_date,
                COALESCE(task_status, 'pending') as task_status,
                COALESCE(is_completed, 0) as is_completed,
                completed_date
              FROM tasks 
              WHERE user_id = :user_id 
              ORDER BY task_goal_date ASC";
            
            $stmt = $db->prepare($query);
            $stmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
            
            if ($stmt->execute()) {
                $tasks = array();
                $task_count = 0;
                
                while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                    $task_count++;
                    $tasks[] = array(
                        "id_task" => (int)$row['id_task'],
                        "user_id" => (int)$row['user_id'],
                        "task_name" => $row['task_name'],
                        "task_type" => $row['task_type'],
                        "task_importance" => $row['task_importance'],
                        "task_goal_date" => $row['task_goal_date'],
                        "notify_start" => $row['notify_start'],
                        "notify_frequency" => $row['notify_frequency'],
                        "notify_type" => $row['notify_type'],
                        "task_note" => $row['task_note'],
                        "task_reward" => (int)$row['task_reward'],
                        "task_creation_date" => $row['task_creation_date'],
                        // ВАЖНО: Добавьте эти поля
                        "task_status" => $row['task_status'],
                        "is_completed" => (bool)$row['is_completed'],
                        "completed_date" => $row['completed_date']
                    );
                }
                
                error_log("Found $task_count tasks for user $user_id");
                
                echo json_encode(array(
                    "success" => true, 
                    "message" => "Tasks loaded successfully",
                    "tasks" => $tasks
                ), JSON_NUMERIC_CHECK);
            } else {
                $error = $stmt->errorInfo();
                error_log("Database query failed: " . print_r($error, true));
                http_response_code(500);
                echo json_encode(array(
                    "message" => "Ошибка базы данных: " . $error[2], 
                    "success" => false
                ));
            }
        } else {
            error_log("Missing user_id parameter");
            http_response_code(400);
            echo json_encode(array(
                "message" => "Не указан user_id", 
                "success" => false
            ));
        }
        break;
        
    case 'POST':
        // Создать задачу
        $data = json_decode(file_get_contents("php://input"));
        error_log("Creating task with data: " . print_r($data, true));
        
        if (!empty($data->user_id) && !empty($data->task_name)) {
            // ВАЖНО: Добавьте поля task_status, is_completed в INSERT
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
                      task_status = :task_status,
                      is_completed = :is_completed,
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
            $notify_type = isset($data->notify_type) ? htmlspecialchars(strip_tags($data->notify_type)) : 'notification';
            $task_note = isset($data->task_note) ? htmlspecialchars(strip_tags($data->task_note)) : NULL;
            $task_reward = isset($data->task_reward) ? intval($data->task_reward) : 0;

            // ВАЖНО: Добавьте значения по умолчанию для новых полей
            $task_status = isset($data->task_status) ? htmlspecialchars(strip_tags($data->task_status)) : 'pending';
            $is_completed = isset($data->is_completed) ? (int)$data->is_completed : 0;
            
            // Привязка параметров
            $stmt->bindParam(":user_id", $user_id, PDO::PARAM_INT);
            $stmt->bindParam(":task_name", $task_name);
            $stmt->bindParam(":task_type", $task_type);
            $stmt->bindParam(":task_importance", $task_importance);
            $stmt->bindParam(":task_goal_date", $task_goal_date);
            $stmt->bindParam(":notify_start", $notify_start);
            $stmt->bindParam(":notify_frequency", $notify_frequency);
            $stmt->bindParam(":notify_type", $notify_type);
            $stmt->bindParam(":task_note", $task_note);
            $stmt->bindParam(":task_reward", $task_reward, PDO::PARAM_INT);
            $stmt->bindParam(":task_status", $task_status);
            $stmt->bindParam(":is_completed", $is_completed, PDO::PARAM_INT);
            
            if ($stmt->execute()) {
                $task_id = $db->lastInsertId();
                error_log("Task created successfully with ID: $task_id");
                
                echo json_encode(array(
                    "message" => "Задача создана успешно",
                    "success" => true,
                    "task_id" => (int)$task_id
                ));
            } else {
                $error = $stmt->errorInfo();
                error_log("Database error: " . print_r($error, true));
                
                http_response_code(503);
                echo json_encode(array(
                    "message" => "Ошибка при создании задачи: " . $error[2],
                    "success" => false
                ));
            }
        } else {
            error_log("Incomplete data for task creation");
            http_response_code(400);
            echo json_encode(array(
                "message" => "Неполные данные. Требуется user_id и task_name",
                "success" => false
            ));
        }
        break;
        
    default:
        error_log("Unsupported method: $method");
        http_response_code(405);
        echo json_encode(array(
            "message" => "Метод не поддерживается", 
            "success" => false
        ));
        break;
}

error_log("=== TASKS API CALL END ===\n");
?>