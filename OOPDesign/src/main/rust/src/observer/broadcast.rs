use tokio::sync::broadcast;

pub struct Notifier<E> {
    sender: broadcast::Sender<E>,
}

impl<E: Clone + Send + 'static> Notifier<E> {
    pub fn new(capacity: usize) -> Self {
        let (tx, _rx) = broadcast::channel(capacity);
        Self { sender: tx }
    }
}