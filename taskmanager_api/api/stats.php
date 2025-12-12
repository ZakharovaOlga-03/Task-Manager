<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

require_once '../config/database.php';
require_once '../models/Task.php';

$database = new Database();
$db = $database->getConnection();
$task = new Task($db);

// Получаем user_id из запроса
$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

if (!$user_id) {
    echo json_encode([
        'success' => false,
        'message' => 'User ID is required'
    ]);
    exit;
}

try {
    // Получаем общую статистику
    $total_tasks = $task->getTotalTasks($user_id);
    $completed_tasks = $task->getCompletedTasks($user_id);
    
    // Получаем статистику по категориям
    $category_stats = $task->getCategoryStats($user_id);
    
    // Формируем ответ
    $response = [
        'success' => true,
        'message' => 'Statistics loaded successfully',
        'total_tasks' => $total_tasks,
        'completed_tasks' => $completed_tasks,
        'categories' => $category_stats
    ];
    
    echo json_encode($response);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error loading statistics: ' . $e->getMessage()
    ]);
}