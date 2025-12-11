<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

// Включаем логирование ошибок
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Логируем входящий запрос
file_put_contents('update_task_status.log', date('Y-m-d H:i:s') . " - Request received\n", FILE_APPEND);
file_put_contents('update_task_status.log', print_r($_SERVER, true) . "\n", FILE_APPEND);
file_put_contents('update_task_status.log', "Request body: " . file_get_contents("php://input") . "\n\n", FILE_APPEND);

require_once '../config/database.php';

try {
    $database = new Database();
    $db = $database->getConnection();
    
    file_put_contents('update_task_status.log', date('Y-m-d H:i:s') . " - Database connected\n", FILE_APPEND);
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $e->getMessage()
    ]);
    exit;
}

// Проверяем метод запроса
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        'success' => false,
        'message' => 'Only POST method is allowed. Method used: ' . $_SERVER['REQUEST_METHOD']
    ]);
    exit;
}

// Получаем данные из POST запроса
$data = json_decode(file_get_contents("php://input"), true);
if (!$data) {
    $data = $_POST;
}

file_put_contents('update_task_status.log', date('Y-m-d H:i:s') . " - Parsed data: " . print_r($data, true) . "\n", FILE_APPEND);

$task_id = isset($data['task_id']) ? intval($data['task_id']) : 0;
$status = isset($data['status']) ? $data['status'] : '';
$is_completed = isset($data['is_completed']) ? filter_var($data['is_completed'], FILTER_VALIDATE_BOOLEAN) : false;

file_put_contents('update_task_status.log', "Task ID: $task_id, Status: $status, Is Completed: " . ($is_completed ? 'true' : 'false') . "\n", FILE_APPEND);

if ($task_id <= 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid task ID: ' . $task_id
    ]);
    exit;
}

try {
    // Сначала проверяем, существует ли задача
    $checkQuery = "SELECT COUNT(*) as count FROM tasks WHERE id_task = :task_id";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(':task_id', $task_id, PDO::PARAM_INT);
    $checkStmt->execute();
    $checkResult = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    file_put_contents('update_task_status.log', "Task exists check: " . $checkResult['count'] . " tasks found\n", FILE_APPEND);
    
    if ($checkResult['count'] == 0) {
        echo json_encode([
            'success' => false,
            'message' => 'Task with ID ' . $task_id . ' not found'
        ]);
        exit;
    }
    
    // Обновляем статус задачи
    $query = "UPDATE tasks SET 
              task_status = :status,
              is_completed = :is_completed,
              completed_date = NOW()
              WHERE id_task = :task_id";
    
    $stmt = $db->prepare($query);
    
    $stmt->bindParam(':task_id', $task_id, PDO::PARAM_INT);
    $stmt->bindParam(':status', $status);
    $stmt->bindParam(':is_completed', $is_completed, PDO::PARAM_BOOL);
    
    if ($stmt->execute()) {
        $rows_affected = $stmt->rowCount();
        
        file_put_contents('update_task_status.log', "Rows affected: $rows_affected\n", FILE_APPEND);
        
        if ($rows_affected > 0) {
            echo json_encode([
                'success' => true,
                'message' => 'Task status updated successfully',
                'task_id' => $task_id
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'message' => 'Task found but no changes made'
            ]);
        }
    } else {
        $errorInfo = $stmt->errorInfo();
        file_put_contents('update_task_status.log', "SQL Error: " . print_r($errorInfo, true) . "\n", FILE_APPEND);
        
        echo json_encode([
            'success' => false,
            'message' => 'Failed to execute update: ' . $errorInfo[2]
        ]);
    }
    
} catch (PDOException $e) {
    file_put_contents('update_task_status.log', "PDO Exception: " . $e->getMessage() . "\n", FILE_APPEND);
    
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    file_put_contents('update_task_status.log', "General Exception: " . $e->getMessage() . "\n", FILE_APPEND);
    
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>