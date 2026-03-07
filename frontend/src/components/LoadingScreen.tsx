interface LoadingScreenProps {
    error?: string | null
}

export function LoadingScreen({ error = null }: LoadingScreenProps) {
    return (
        <div className="loading-screen">
            <div className="loading-screen__bars">
                {[0, 1, 2, 3, 4, 5, 6].map((i) => (
                    <div
                        key={i}
                        className="loading-screen__bar"
                        style={{
                            animation: `bar-grow 1.1s ease-in-out ${i * 0.1}s infinite alternate`,
                            height: 10 + i * 4,
                        }}
                    />
                ))}
            </div>

            {error ? (
                <p className="loading-screen__error">✕ {error}</p>
            ) : (
                <p className="loading-screen__label">connecting to coordinator…</p>
            )}
        </div>
    )
}